"""
Wykrywanie i maskowanie danych wrażliwych przed wysłaniem treści do modelu AI.

Zasada: lepiej zamaskować za dużo niż za mało, ale identyfikatory z sumą
kontrolną (PESEL, NIP, IBAN, karta) są walidowane, żeby ograniczyć fałszywe alarmy.

Tylko biblioteka standardowa — hook musi działać na każdej maszynie bez instalacji.
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Callable, Optional


@dataclass(frozen=True)
class Finding:
    kind: str
    start: int
    end: int
    value: str


# ---------------------------------------------------------------- walidatory

def pesel_valid(digits: str) -> bool:
    if len(digits) != 11 or not digits.isdigit():
        return False
    weights = (1, 3, 7, 9, 1, 3, 7, 9, 1, 3)
    checksum = sum(int(d) * w for d, w in zip(digits[:10], weights))
    if (10 - checksum % 10) % 10 != int(digits[10]):
        return False
    month = int(digits[2:4])
    return month % 20 in range(1, 13)  # miesiąc koduje stulecie (+20, +40, +60, +80)


def nip_valid(raw: str) -> bool:
    digits = re.sub(r"\D", "", raw)
    if len(digits) != 10:
        return False
    weights = (6, 5, 7, 2, 3, 4, 5, 6, 7)
    control = sum(int(d) * w for d, w in zip(digits[:9], weights)) % 11
    return control != 10 and control == int(digits[9])


def iban_valid(raw: str) -> bool:
    iban = re.sub(r"\s", "", raw).upper()
    if not re.fullmatch(r"[A-Z]{2}\d{2}[A-Z0-9]{10,30}", iban):
        return False
    rearranged = iban[4:] + iban[:4]
    numeric = "".join(str(int(ch, 36)) for ch in rearranged)
    return int(numeric) % 97 == 1


def luhn_valid(raw: str) -> bool:
    digits = [int(d) for d in re.sub(r"\D", "", raw)]
    if not 13 <= len(digits) <= 19:
        return False
    total = 0
    for i, d in enumerate(reversed(digits)):
        if i % 2 == 1:
            d *= 2
            if d > 9:
                d -= 9
        total += d
    return total % 10 == 0


# ---------------------------------------------------------------- reguły
# Kolejność ma znaczenie: dłuższe / bardziej specyficzne identyfikatory najpierw,
# żeby np. numer konta nie został pocięty przez regułę telefonu.

Rule = tuple[str, re.Pattern, Optional[Callable[[str], bool]]]

RULES: list[Rule] = [
    ("IBAN", re.compile(r"\b(?:PL)?\s?\d{2}(?:[ ]?\d{4}){6}\b", re.I),
     lambda v: iban_valid(v if v.upper().startswith("PL") else "PL" + v)),
    ("CARD", re.compile(r"\b(?:\d[ -]?){13,19}\b"), luhn_valid),
    ("PESEL", re.compile(r"(?<!\d)\d{11}(?!\d)"), pesel_valid),
    ("NIP", re.compile(r"(?<!\d)\d{3}-?\d{3}-?\d{2}-?\d{2}(?!\d)"), nip_valid),
    ("EMAIL", re.compile(r"\b[\w.+-]+@[\w-]+(?:\.[\w-]+)+\b"), None),
    # Numer polisy — format syntetyczny używany w projekcie demo (np. POL-2024-00012345).
    # W realnym projekcie wzorzec pochodzi z katalogu danych organizacji.
    ("POLICY_NO", re.compile(r"\bPOL-\d{4}-\d{8}\b"), None),
    ("CLAIM_NO", re.compile(r"\bSZK-\d{4}-\d{6}\b"), None),
    ("PHONE", re.compile(r"(?<![\w+])(?:\+48[ -]?)?\d{3}[ -]?\d{3}[ -]?\d{3}(?!\d)"), None),
    ("REG_PLATE", re.compile(r"\b[A-Z]{2,3}[ ]?\d[A-Z0-9]{3,4}\b"), lambda v: any(c.isdigit() for c in v[3:])),
]


def find_sensitive(text: str) -> list[Finding]:
    findings: list[Finding] = []
    taken: list[tuple[int, int]] = []
    for kind, pattern, validator in RULES:
        for m in pattern.finditer(text):
            start, end = m.span()
            if any(start < e and end > s for s, e in taken):
                continue
            value = m.group(0)
            if validator and not validator(value):
                continue
            findings.append(Finding(kind, start, end, value))
            taken.append((start, end))
    return sorted(findings, key=lambda f: f.start)


def mask(text: str) -> tuple[str, list[Finding]]:
    findings = find_sensitive(text)
    out, last = [], 0
    for f in findings:
        out.append(text[last:f.start])
        out.append(f"[{f.kind}]")
        last = f.end
    out.append(text[last:])
    return "".join(out), findings
