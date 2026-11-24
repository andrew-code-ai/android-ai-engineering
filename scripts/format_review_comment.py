#!/usr/bin/env python3
"""Zamienia wynik `claude -p --output-format json` na komentarz Markdown do PR."""
import json
import re
import sys

SEVERITY_ICON = {"high": "🔴", "medium": "🟠", "low": "🟡"}


def extract_json(text: str) -> dict:
    fenced = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", text, re.S)
    candidate = fenced.group(1) if fenced else text[text.find("{"): text.rfind("}") + 1]
    return json.loads(candidate)


def main(path: str) -> None:
    raw = json.load(open(path, encoding="utf-8"))
    result_text = raw.get("result", "") if isinstance(raw, dict) else str(raw)
    cost = raw.get("total_cost_usd") if isinstance(raw, dict) else None
    try:
        review = extract_json(result_text)
    except (ValueError, json.JSONDecodeError):
        print("### 🤖 AI review\n\nNie udało się sparsować wyniku agenta:\n\n```\n" + result_text[:3000] + "\n```")
        return

    findings = sorted(review.get("findings", []),
                      key=lambda f: ["high", "medium", "low"].index(f.get("severity", "low")))
    lines = ["### 🤖 AI review (android-reviewer)", "", review.get("summary", ""), ""]
    if not findings:
        lines.append("Brak istotnych uwag. ✅")
    else:
        lines += ["| | Plik:linia | Kategoria | Problem | Propozycja |", "|---|---|---|---|---|"]
        for f in findings:
            lines.append(f"| {SEVERITY_ICON.get(f.get('severity'), '')} | `{f.get('file', '?')}:{f.get('line', '?')}` "
                         f"| {f.get('category', '')} | **{f.get('title', '')}** — {f.get('why', '')} | {f.get('fix', '')} |")
    if cost is not None:
        lines += ["", f"<sub>Koszt przeglądu: ${cost:.4f}</sub>"]
    lines += ["", "<sub>Komentarz wygenerowany przez AI — decyzję podejmuje reviewer.</sub>"]
    print("\n".join(lines))


if __name__ == "__main__":
    main(sys.argv[1])
