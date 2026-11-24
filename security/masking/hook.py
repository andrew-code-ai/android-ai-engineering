#!/usr/bin/env python3
"""
Hook Claude Code egzekwujący AI-usage-policy.md.

UserPromptSubmit — jeśli prompt zawiera dane wrażliwe (PESEL, IBAN, karta, NIP,
                   e-mail, telefon, nr polisy/szkody, nr rejestracyjny), prompt jest
                   blokowany, a użytkownik dostaje wersję zamaskowaną do ponownego wysłania.
PreToolUse       — blokuje odczyt plików z klasy „dane klientów” (ścieżki z
                   security/restricted-paths.txt) przez Read/Grep/Glob/Bash.

Kod wyjścia 2 = blokada (Claude Code pokazuje stderr). Log decyzji zapisuje
wyłącznie typy wykrytych danych, nigdy wartości.
"""
from __future__ import annotations

import fnmatch
import json
import os
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from masking import mask  # noqa: E402

PROJECT_DIR = Path(os.environ.get("CLAUDE_PROJECT_DIR", Path(__file__).resolve().parents[2]))
RESTRICTED_FILE = PROJECT_DIR / "security" / "restricted-paths.txt"
LOG_FILE = PROJECT_DIR / "security" / "logs" / "decisions.jsonl"


def log(event: str, decision: str, kinds: list[str], detail: str = "") -> None:
    try:
        LOG_FILE.parent.mkdir(parents=True, exist_ok=True)
        with LOG_FILE.open("a", encoding="utf-8") as fh:
            fh.write(json.dumps({"ts": time.strftime("%Y-%m-%dT%H:%M:%S"), "event": event,
                                 "decision": decision, "kinds": sorted(set(kinds)),
                                 "detail": detail}, ensure_ascii=False) + "\n")
    except OSError:
        pass  # log nie może blokować pracy


def restricted_patterns() -> list[str]:
    if not RESTRICTED_FILE.exists():
        return []
    return [ln.strip() for ln in RESTRICTED_FILE.read_text(encoding="utf-8").splitlines()
            if ln.strip() and not ln.startswith("#")]


def touches_restricted(tool_input: dict) -> str | None:
    candidates = [str(tool_input.get(k, "")) for k in ("file_path", "path", "pattern", "command")]
    for pattern in restricted_patterns():
        for value in candidates:
            if not value:
                continue
            rel = value.replace(str(PROJECT_DIR) + os.sep, "")
            if fnmatch.fnmatch(rel, pattern) or pattern.rstrip("*").rstrip("/") in value:
                return pattern
    return None


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except json.JSONDecodeError:
        return 0
    event = payload.get("hook_event_name", "")

    if event == "UserPromptSubmit":
        masked, findings = mask(payload.get("prompt", ""))
        if not findings:
            return 0
        kinds = [f.kind for f in findings]
        log(event, "block", kinds)
        print("Zablokowano: prompt zawiera dane wrażliwe "
              f"({', '.join(sorted(set(kinds)))}). Zgodnie z AI-usage-policy.md "
              "takie dane nie mogą trafić do modelu.\n\n"
              "Wersja zamaskowana do ponownego wysłania:\n" + masked, file=sys.stderr)
        return 2

    if event == "PreToolUse":
        hit = touches_restricted(payload.get("tool_input", {}) or {})
        if hit:
            log(event, "block", ["RESTRICTED_PATH"], f"{payload.get('tool_name')}:{hit}")
            print(f"Zablokowano: '{hit}' to dane klasy D (dane klientów) — "
                  "AI nie może ich odczytywać. Użyj danych syntetycznych z /app/src/test/fixtures.",
                  file=sys.stderr)
            return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
