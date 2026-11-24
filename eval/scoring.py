"""Porównanie znalezisk agenta z oczekiwanymi wadami (precision / recall)."""
from __future__ import annotations

import json
import re
from dataclasses import dataclass, field

LINE_TOLERANCE = 1  # agent może wskazać linię obok — to nadal ta sama wada


@dataclass
class CaseScore:
    case: str
    expected: int
    reported: int
    true_positives: int
    category_hits: int
    missed: list[str] = field(default_factory=list)
    false_positives: list[dict] = field(default_factory=list)

    @property
    def recall(self) -> float:
        return self.true_positives / self.expected if self.expected else 1.0

    @property
    def precision(self) -> float:
        return self.true_positives / self.reported if self.reported else 1.0


def extract_json(text: str) -> dict:
    """Model czasem owija JSON w ```json ...``` albo dodaje zdanie — wyciągamy sam obiekt."""
    fenced = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", text, re.S)
    candidate = fenced.group(1) if fenced else text[text.find("{"): text.rfind("}") + 1]
    return json.loads(candidate)


def score_case(case: str, expected: list[dict], findings: list[dict]) -> CaseScore:
    """Dopasowanie zachłanne 1:1 — każda wada i każde znalezisko liczą się najwyżej raz.
    Najpierw dopasowania ze zgodną kategorią, potem po samej linii."""
    unmatched_expected = list(expected)
    tp, cat_hits, fps = 0, 0, []

    def candidates(f: dict) -> list[dict]:
        line = f.get("line")
        if not isinstance(line, int):
            return []
        return [e for e in unmatched_expected
                if any(abs(line - l) <= LINE_TOLERANCE for l in e["lines"])]

    remaining = []
    for f in findings:  # przebieg 1: linia + kategoria
        same_cat = [e for e in candidates(f) if e["category"] == f.get("category")]
        if same_cat:
            unmatched_expected.remove(same_cat[0])
            tp += 1
            cat_hits += 1
        else:
            remaining.append(f)
    for f in remaining:  # przebieg 2: sama linia
        c = candidates(f)
        if c:
            unmatched_expected.remove(c[0])
            tp += 1
        else:
            fps.append(f)

    return CaseScore(case=case, expected=len(expected), reported=len(findings), true_positives=tp,
                     category_hits=cat_hits, missed=[e["id"] for e in unmatched_expected], false_positives=fps)


def aggregate(scores: list[CaseScore]) -> dict:
    exp = sum(s.expected for s in scores)
    rep = sum(s.reported for s in scores)
    tp = sum(s.true_positives for s in scores)
    return {
        "expected": exp, "reported": rep, "true_positives": tp,
        "recall": tp / exp if exp else 1.0,
        "precision": tp / rep if rep else 1.0,
        "category_accuracy": sum(s.category_hits for s in scores) / tp if tp else 0.0,
        "false_positives_on_clean_file": sum(len(s.false_positives) for s in scores if s.expected == 0),
    }
