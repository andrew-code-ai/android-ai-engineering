"""Testy maskowania. Wszystkie dane są syntetyczne (poprawne sumy kontrolne, fikcyjne osoby)."""
import json
import os
import subprocess
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent))
from masking import iban_valid, luhn_valid, mask, nip_valid, pesel_valid  # noqa: E402

HOOK = Path(__file__).resolve().parent / "hook.py"

PESEL = "44051401359"                       # przykładowy, poprawna suma kontrolna
IBAN = "PL61 1090 1014 0000 0712 1981 2874"  # przykładowy numer z dokumentacji bankowej
NIP = "526-000-12-46"                       # syntetyczny, poprawna suma kontrolna
CARD = "4111 1111 1111 1111"                # testowy numer Visa


@pytest.mark.parametrize("value,ok", [(PESEL, True), ("44051401358", False), ("12345678901", False)])
def test_pesel_checksum(value, ok):
    assert pesel_valid(value) is ok


def test_iban_checksum():
    assert iban_valid(IBAN)
    assert not iban_valid(IBAN[:-1] + "5")


def test_nip_and_card_checksum():
    assert nip_valid(NIP)
    assert not nip_valid("526-000-12-47")
    assert luhn_valid(CARD)
    assert not luhn_valid("4111 1111 1111 1112")


def test_masks_all_kinds_in_claim_description():
    text = (f"Klient PESEL {PESEL}, konto {IBAN}, NIP {NIP}, karta {CARD}, "
            "mail jan.kowalski@example.com, tel. +48 600 700 800, polisa POL-2024-00012345, "
            "szkoda SZK-2025-000123, auto WX 12345.")
    masked, findings = mask(text)
    kinds = {f.kind for f in findings}
    assert kinds == {"PESEL", "IBAN", "NIP", "CARD", "EMAIL", "PHONE", "POLICY_NO", "CLAIM_NO", "REG_PLATE"}
    for raw in (PESEL, IBAN, NIP, CARD, "jan.kowalski@example.com", "POL-2024-00012345"):
        assert raw not in masked


def test_invalid_checksum_numbers_are_not_masked():
    masked, findings = mask("Build 12345678901 zakończony, commit 44051401358")
    assert findings == []
    assert masked.startswith("Build 12345678901")


def test_code_without_personal_data_passes():
    code = "val state = MutableStateFlow(CartUiState())\nfun total(): Money = Money.ofCents(20_000)"
    assert mask(code)[1] == []


def run_hook(payload: dict, project_dir: Path) -> subprocess.CompletedProcess:
    env = {**os.environ, "CLAUDE_PROJECT_DIR": str(project_dir)}
    return subprocess.run([sys.executable, str(HOOK)], input=json.dumps(payload),
                          capture_output=True, text=True, env=env)


def test_hook_blocks_prompt_with_pesel(tmp_path):
    r = run_hook({"hook_event_name": "UserPromptSubmit", "prompt": f"Napraw walidację dla {PESEL}"}, tmp_path)
    assert r.returncode == 2
    assert "[PESEL]" in r.stderr and PESEL not in r.stderr
    log = (tmp_path / "security/logs/decisions.jsonl").read_text()
    assert PESEL not in log and "PESEL" in log


def test_hook_allows_clean_prompt(tmp_path):
    r = run_hook({"hook_event_name": "UserPromptSubmit", "prompt": "Zrefaktoruj CartViewModel"}, tmp_path)
    assert r.returncode == 0


def test_hook_blocks_restricted_path(tmp_path):
    (tmp_path / "security").mkdir()
    (tmp_path / "security/restricted-paths.txt").write_text("data/customers/*\n")
    r = run_hook({"hook_event_name": "PreToolUse", "tool_name": "Read",
                  "tool_input": {"file_path": str(tmp_path / "data/customers/export.csv")}}, tmp_path)
    assert r.returncode == 2
    assert "dane klientów" in r.stderr
