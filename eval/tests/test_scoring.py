import json
import subprocess
import sys
from pathlib import Path

EVAL = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(EVAL))
from scoring import aggregate, extract_json, score_case  # noqa: E402

EXPECTED = [
    {"id": "A", "lines": [10], "category": "concurrency"},
    {"id": "B", "lines": [20, 30], "category": "state"},
]


def test_perfect_match():
    s = score_case("x", EXPECTED, [{"line": 10, "category": "concurrency"}, {"line": 30, "category": "state"}])
    assert (s.true_positives, s.recall, s.precision, s.category_hits) == (2, 1.0, 1.0, 2)


def test_line_tolerance_and_wrong_category_still_counts():
    s = score_case("x", EXPECTED, [{"line": 11, "category": "architecture"}])
    assert s.true_positives == 1 and s.category_hits == 0 and s.missed == ["B"]


def test_duplicate_findings_are_false_positives():
    s = score_case("x", EXPECTED, [{"line": 10, "category": "concurrency"}, {"line": 10, "category": "concurrency"}])
    assert s.true_positives == 1 and len(s.false_positives) == 1 and s.precision == 0.5


def test_clean_file_false_positive_counted():
    agg = aggregate([score_case("clean", [], [{"line": 3, "category": "style"}])])
    assert agg["false_positives_on_clean_file"] == 1


def test_extract_json_from_fenced_answer():
    assert extract_json('Here you go:\n```json\n{"findings": []}\n```')["findings"] == []


def test_dry_run_end_to_end(tmp_path):
    r = subprocess.run([sys.executable, str(EVAL / "run_eval.py"), "--variants", "all", "--dry-run",
                        "--out", str(tmp_path)], capture_output=True, text=True)
    assert r.returncode == 0, r.stderr
    report = (tmp_path / "report.md").read_text()
    assert "SYMULACJA" in report and "| routing |" in report and "| batch |" in report
    data = json.loads(next(tmp_path.glob("*-dryrun.json")).read_text())
    by_variant = {s["variant"]: s for s in data["summaries"]}
    assert by_variant["caching"]["cache_read_tokens"] > 0
    assert "submit_claim_usecase" in by_variant["routing"]["skipped_by_triage"]
