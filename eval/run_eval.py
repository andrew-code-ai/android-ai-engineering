#!/usr/bin/env python3
"""
Audyt kosztu i jakości agenta android-reviewer.

Ten sam zestaw plików z wadami (eval/dataset) przeglądany jest w 4 wariantach:

  baseline  — mocny model, pełny prompt przy każdym wywołaniu
  caching   — jak baseline, ale stały prefiks (skill + checklisty) oznaczony cache_control
  routing   — tani model ocenia, czy plik wymaga przeglądu; mocny model (z cache) tylko dla ryzykownych
  batch     — jak caching, ale przez Message Batches API (asynchronicznie, z rabatem)

Wynik: eval/results/<czas>.json + eval/results/report.md (tabela koszt / recall / precision).

  python3 eval/run_eval.py --variants all            # wymaga ANTHROPIC_API_KEY
  python3 eval/run_eval.py --variants all --dry-run  # symulacja bez API — do sprawdzenia pipeline'u
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import time
from dataclasses import asdict, dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EVAL = ROOT / "eval"
sys.path.insert(0, str(EVAL))
from scoring import aggregate, score_case  # noqa: E402

SKILL_DIR = ROOT / ".claude" / "skills" / "android-architecture-review"
VARIANTS = ["baseline", "caching", "routing", "batch"]

TRIAGE_PROMPT = """You triage Kotlin files for an expensive architecture review.
Answer by calling the triage_decision tool. needs_review=false ONLY if the file is small, has no state, no coroutines, no I/O, no money,
no personal data and follows clean architecture. When unsure, answer true."""


# ------------------------------------------------------------------ dane

@dataclass
class Case:
    name: str
    path: Path
    code: str
    expected: list[dict]

    @property
    def numbered(self) -> str:
        return "\n".join(f"{i:4d}| {line}" for i, line in enumerate(self.code.splitlines(), 1))


def load_cases(only: list[str] | None) -> list[Case]:
    cases = []
    for d in sorted((EVAL / "dataset").iterdir()):
        if not d.is_dir() or (only and d.name not in only):
            continue
        exp = json.loads((d / "expected.json").read_text(encoding="utf-8"))
        src = d / exp["file"]
        cases.append(Case(d.name, src, src.read_text(encoding="utf-8"), exp["defects"]))
    return cases


def review_system_prompt() -> str:
    parts = [(SKILL_DIR / "SKILL.md").read_text(encoding="utf-8")]
    for ref in sorted((SKILL_DIR / "references").glob("*.md")):
        parts.append(f"\n\n<reference file=\"{ref.name}\">\n{ref.read_text(encoding='utf-8')}\n</reference>")
    return "".join(parts)


def user_message(case: Case) -> str:
    return (f"Review this file and report via the report_findings tool (keep text fields short, no code). "
            f"Line numbers are prefixed as 'NNNN| '. Report `line` using those numbers.\n"
            f"File: {case.path.relative_to(ROOT)}\n\n```kotlin\n{case.numbered}\n```")


# ------------------------------------------------------------------ koszt

@dataclass
class Usage:
    model: str
    input_tokens: int = 0
    output_tokens: int = 0
    cache_write_tokens: int = 0
    cache_read_tokens: int = 0
    batch: bool = False

    def cost(self, cfg: dict) -> float | None:
        price = cfg["pricing_per_mtok"].get(self.model, {})
        if price.get("input") is None or price.get("output") is None:
            return None
        m = cfg["multipliers"]
        cache_write = price.get("cache_write_5m", price["input"] * m["cache_write"])
        cache_read = price.get("cache_read", price["input"] * m["cache_read"])
        usd = (self.input_tokens * price["input"]
               + self.cache_write_tokens * cache_write
               + self.cache_read_tokens * cache_read
               + self.output_tokens * price["output"]) / 1_000_000
        return usd * (m["batch"] if self.batch else 1.0)


def usage_from(resp, model: str, batch: bool = False) -> Usage:
    u = resp.usage
    return Usage(model, u.input_tokens, u.output_tokens,
                 getattr(u, "cache_creation_input_tokens", 0) or 0,
                 getattr(u, "cache_read_input_tokens", 0) or 0, batch)


@dataclass
class CaseRun:
    case: str
    findings: list[dict]
    usages: list[Usage] = field(default_factory=list)
    skipped_by_triage: bool = False
    error: str | None = None


# ------------------------------------------------------------------ ustrukturyzowany wynik
# Model raportuje przez wymuszone wywołanie narzędzia (tool use + tool_choice), więc dostajemy
# gotowy obiekt zgodny ze schematem — bez parsowania tekstu, w którym kod Kotlina z cudzysłowami
# psuł JSON. Obcięcie na limicie tokenów (stop_reason == "max_tokens") wykrywamy jawnie.

CATEGORIES = ["architecture", "state", "concurrency", "domain", "error_handling",
              "testability", "compose", "security", "duplication"]

REVIEW_TOOL = {
    "name": "report_findings",
    "description": "Report all review findings for the file. Call exactly once.",
    "input_schema": {
        "type": "object",
        "properties": {
            "findings": {"type": "array", "items": {
                "type": "object",
                "properties": {
                    "line": {"type": "integer"},
                    "category": {"type": "string", "enum": CATEGORIES},
                    "severity": {"type": "string", "enum": ["high", "medium", "low"]},
                    "title": {"type": "string", "description": "max 12 words"},
                    "why": {"type": "string", "description": "max 30 words, no code"},
                    "fix": {"type": "string", "description": "max 25 words, no code"},
                },
                "required": ["line", "category", "severity", "title"],
            }},
            "summary": {"type": "string"},
        },
        "required": ["findings", "summary"],
    },
}

TRIAGE_TOOL = {
    "name": "triage_decision",
    "description": "Decide whether the file needs the expensive architecture review.",
    "input_schema": {
        "type": "object",
        "properties": {"needs_review": {"type": "boolean"}, "reason": {"type": "string"}},
        "required": ["needs_review", "reason"],
    },
}


class Truncated(ValueError):
    pass


def tool_input(msg, tool_name: str) -> dict:
    if getattr(msg, "stop_reason", None) == "max_tokens":
        raise Truncated("odpowiedź obcięta na max_tokens — zwiększ max_tokens w config.json")
    for block in msg.content:
        if getattr(block, "type", "") == "tool_use" and block.name == tool_name:
            return dict(block.input)
    raise ValueError(f"brak wywołania narzędzia {tool_name}")


# ------------------------------------------------------------------ klient (prawdziwy lub symulowany)

class LiveClient:
    def __init__(self, cfg: dict):
        import anthropic  # importujemy dopiero tutaj — dry-run nie wymaga SDK
        self.api = anthropic.Anthropic()
        self.cfg = cfg
        self.fatal_errors = (anthropic.AuthenticationError, anthropic.PermissionDeniedError)
        try:  # preflight: zły klucz wychodzi od razu, a nie po 3 wariantach
            self.api.models.list(limit=1)
        except self.fatal_errors as e:
            raise SystemExit(f"Klucz API odrzucony ({type(e).__name__}). Sprawdź ANTHROPIC_API_KEY "
                             "(platform.claude.com → API keys) i środki na koncie.") from None

    def params(self, model: str, system: str, user: str, cache: bool, tool: dict,
               max_tokens: int | None = None) -> dict:
        block = {"type": "text", "text": system}
        if cache:  # cache obejmuje prefiks: definicję narzędzia + prompt systemowy
            block["cache_control"] = {"type": "ephemeral"}
        return {"model": model, "max_tokens": max_tokens or self.cfg["max_tokens"], "system": [block],
                "tools": [tool], "tool_choice": {"type": "tool", "name": tool["name"]},
                "messages": [{"role": "user", "content": user}]}

    def call(self, model, system, user, cache, tool, max_tokens=None) -> tuple[dict, Usage]:
        resp = self.api.messages.create(**self.params(model, system, user, cache, tool, max_tokens))
        usage = usage_from(resp, model)
        try:
            return tool_input(resp, tool["name"]), usage
        except ValueError as e:
            e.usage = usage  # koszt nieudanego wywołania też liczymy
            raise

    def batch(self, requests: list[tuple[str, dict]]) -> dict[str, tuple[dict | None, Usage | None, str | None]]:
        model_by_id = {cid: params["model"] for cid, params in requests}
        tool_by_id = {cid: params["tools"][0]["name"] for cid, params in requests}
        created = self.api.messages.batches.create(
            requests=[{"custom_id": cid, "params": params} for cid, params in requests])
        deadline = time.time() + self.cfg["batch_timeout_minutes"] * 60
        while True:
            b = self.api.messages.batches.retrieve(created.id)
            if b.processing_status == "ended":
                break
            if time.time() > deadline:
                raise TimeoutError(f"Batch {created.id} not finished — sprawdź później w konsoli")
            print(f"  batch {created.id}: {b.processing_status}…", file=sys.stderr)
            time.sleep(self.cfg["batch_poll_seconds"])
        out = {}
        for entry in self.api.messages.batches.results(created.id):
            cid = entry.custom_id
            if entry.result.type != "succeeded":
                out[cid] = (None, None, entry.result.type)
                continue
            msg = entry.result.message
            usage = usage_from(msg, model_by_id[cid], batch=True)
            try:
                out[cid] = (tool_input(msg, tool_by_id[cid]), usage, None)
            except ValueError as e:
                out[cid] = (None, usage, str(e))
        return out


class SimulatedClient(LiveClient):
    """Deterministyczna symulacja — sprawdza pipeline, scoring i raport bez kosztów.
    Liczby z trybu --dry-run NIE są wynikami modelu i raport tak je oznacza."""
    fatal_errors = ()

    def __init__(self, cfg: dict, cases: list[Case]):
        self.cfg = cfg
        self.by_text = {c.numbered: c for c in cases}
        self.cache_warm = False

    def _case(self, user: str) -> Case:
        return next(c for t, c in self.by_text.items() if t in user)

    @staticmethod
    def _keep(defect_id: str) -> bool:  # ~85% wad "wykrytych", stabilnie między uruchomieniami
        return int(hashlib.md5(defect_id.encode()).hexdigest(), 16) % 100 < 85

    def _fake(self, model, system, user, cache, tool, batch=False) -> tuple[dict, Usage]:
        case = self._case(user)
        if tool["name"] == TRIAGE_TOOL["name"]:
            return {"needs_review": bool(case.expected), "reason": "simulated"}, \
                Usage(model, len(system + user) // 4, 20, batch=batch)
        findings = [{"line": d["lines"][0], "category": d["category"], "severity": "medium",
                     "title": d["title"]} for d in case.expected if self._keep(d["id"])]
        if not case.expected:
            findings.append({"line": 3, "category": "duplication", "severity": "low", "title": "simulated FP"})
        sys_tokens, user_tokens = len(system) // 4, len(user) // 4
        out_tokens = 60 * len(findings) + 40
        if cache and self.cache_warm:
            usage = Usage(model, user_tokens, out_tokens, 0, sys_tokens, batch)
        elif cache:
            self.cache_warm = True
            usage = Usage(model, user_tokens, out_tokens, sys_tokens, 0, batch)
        else:
            usage = Usage(model, sys_tokens + user_tokens, out_tokens, batch=batch)
        return {"findings": findings, "summary": "simulated"}, usage

    def call(self, model, system, user, cache, tool, max_tokens=None):
        return self._fake(model, system, user, cache, tool)

    def batch(self, requests):
        out = {}
        for cid, p in requests:
            data, usage = self._fake(p["model"], p["system"][0]["text"], p["messages"][0]["content"],
                                     "cache_control" in p["system"][0], p["tools"][0], batch=True)
            out[cid] = (data, usage, None)
        return out


# ------------------------------------------------------------------ warianty

def run_variant(variant: str, client: LiveClient, cases: list[Case], cfg: dict) -> list[CaseRun]:
    system = review_system_prompt()
    review_model, triage_model = cfg["models"]["review"], cfg["models"]["triage"]
    if isinstance(client, SimulatedClient):
        client.cache_warm = False
    runs: list[CaseRun] = []

    if variant == "batch":
        reqs = [(c.name, client.params(review_model, system, user_message(c), cache=True, tool=REVIEW_TOOL))
                for c in cases]
        try:
            results = client.batch(reqs)
        except client.fatal_errors:
            raise
        except Exception as e:
            return [CaseRun(c.name, [], error=f"batch: {type(e).__name__}: {e}") for c in cases]
        for c in cases:
            data, usage, err = results.get(c.name, (None, None, "missing"))
            runs.append(CaseRun(c.name, (data or {}).get("findings", []), [usage] if usage else [], error=err))
        return runs

    for c in cases:
        usages: list[Usage] = []
        try:
            if variant == "routing":
                decision, t_usage = client.call(triage_model, TRIAGE_PROMPT, user_message(c), cache=False,
                                                tool=TRIAGE_TOOL, max_tokens=300)
                usages.append(t_usage)
                if not decision.get("needs_review", True):
                    runs.append(CaseRun(c.name, [], usages, skipped_by_triage=True))
                    continue
            data, usage = client.call(review_model, system, user_message(c),
                                      cache=variant in ("caching", "routing"), tool=REVIEW_TOOL)
            usages.append(usage)
            runs.append(CaseRun(c.name, data.get("findings", []), usages))
        except client.fatal_errors:
            raise
        except Exception as e:  # jeden zły przypadek nie przerywa audytu
            if getattr(e, "usage", None):
                usages.append(e.usage)
            runs.append(CaseRun(c.name, [], usages, error=f"{type(e).__name__}: {e}"))
    return runs


# ------------------------------------------------------------------ raport

def summarize(variant: str, runs: list[CaseRun], cases: list[Case], cfg: dict) -> dict:
    by_name = {c.name: c for c in cases}
    scores = [score_case(r.case, by_name[r.case].expected, r.findings) for r in runs]
    usages = [u for r in runs for u in r.usages]
    costs = [u.cost(cfg) for u in usages]
    return {
        "variant": variant,
        **aggregate(scores),
        "input_tokens": sum(u.input_tokens for u in usages),
        "cache_write_tokens": sum(u.cache_write_tokens for u in usages),
        "cache_read_tokens": sum(u.cache_read_tokens for u in usages),
        "output_tokens": sum(u.output_tokens for u in usages),
        "cost_usd": None if any(c is None for c in costs) else round(sum(costs), 5),
        "skipped_by_triage": [r.case for r in runs if r.skipped_by_triage],
        "errors": {r.case: r.error for r in runs if r.error},
        "per_case": [{**asdict(s), "recall": round(s.recall, 3), "precision": round(s.precision, 3)} for s in scores],
    }


def write_report(summaries: list[dict], simulated: bool, out_dir: Path) -> Path:
    base = next((s["cost_usd"] for s in summaries if s["variant"] == "baseline"), None)
    lines = ["# Audyt kosztu i jakości — android-reviewer", ""]
    if simulated:
        lines += ["> ⚠️ **SYMULACJA (--dry-run)** — liczby nie pochodzą z modelu. Uruchom bez `--dry-run`, żeby uzyskać realne wyniki.", ""]
    lines += [f"Data: {time.strftime('%Y-%m-%d %H:%M')}", "",
              "| Wariant | Recall | Precision | Kategoria trafna | FP na czystym pliku | Tokeny in / cache W / cache R / out | Koszt USD | vs baseline |",
              "|---|---|---|---|---|---|---|---|"]
    for s in summaries:
        cost = "n/a" if s["cost_usd"] is None else f"{s['cost_usd']:.4f}"
        delta = "—" if (base in (None, 0) or s["cost_usd"] is None or s["variant"] == "baseline") \
            else f"{(s['cost_usd'] / base - 1) * 100:+.0f}%"
        lines.append(f"| {s['variant']} | {s['recall']:.0%} | {s['precision']:.0%} | {s['category_accuracy']:.0%} | "
                     f"{s['false_positives_on_clean_file']} | {s['input_tokens']} / {s['cache_write_tokens']} / "
                     f"{s['cache_read_tokens']} / {s['output_tokens']} | {cost} | {delta} |")
    if any(s["cost_usd"] is None for s in summaries):
        lines += ["", "Koszt `n/a`: uzupełnij ceny w `eval/config.json`."]
    for s in summaries:
        if s["skipped_by_triage"]:
            lines.append(f"\n- `{s['variant']}`: triage pominął {', '.join(s['skipped_by_triage'])}")
        if s["errors"]:
            lines.append(f"\n- `{s['variant']}` błędy: {s['errors']}")
    lines += ["", "## Pominięte wady (wariant baseline)", ""]
    for s in summaries:
        if s["variant"] == "baseline":
            for pc in s["per_case"]:
                lines.append(f"- **{pc['case']}**: recall {pc['recall']:.0%}, pominięte: {', '.join(pc['missed']) or '—'}")
    path = out_dir / "report.md"
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--variants", default="all", help=f"all lub lista: {','.join(VARIANTS)}")
    ap.add_argument("--cases", default="", help="nazwy folderów z eval/dataset, po przecinku (domyślnie wszystkie)")
    ap.add_argument("--dry-run", action="store_true", help="symulacja bez wywołań API")
    ap.add_argument("--out", default=str(EVAL / "results"))
    args = ap.parse_args()

    cfg = json.loads((EVAL / "config.json").read_text(encoding="utf-8"))
    variants = VARIANTS if args.variants == "all" else [v.strip() for v in args.variants.split(",")]
    cases = load_cases([c for c in args.cases.split(",") if c] or None)
    if not args.dry_run and not os.environ.get("ANTHROPIC_API_KEY"):
        print("Brak ANTHROPIC_API_KEY. Użyj --dry-run albo ustaw klucz.", file=sys.stderr)
        return 1
    client = SimulatedClient(cfg, cases) if args.dry_run else LiveClient(cfg)

    summaries = []
    for v in variants:
        print(f"▶ {v} ({len(cases)} plików)…", file=sys.stderr)
        summaries.append(summarize(v, run_variant(v, client, cases, cfg), cases, cfg))

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    stamp = time.strftime("%Y%m%d-%H%M%S")
    (out / f"{stamp}{'-dryrun' if args.dry_run else ''}.json").write_text(
        json.dumps({"simulated": args.dry_run, "config": cfg, "summaries": summaries}, indent=2, ensure_ascii=False),
        encoding="utf-8")
    report = write_report(summaries, args.dry_run, out)
    print(report.read_text(encoding="utf-8"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
