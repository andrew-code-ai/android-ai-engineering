#!/usr/bin/env bash
# Uruchamia audyt kosztu i jakości agenta. Argumenty przekazywane do eval/run_eval.py.
#   ./scripts/run-eval.sh --dry-run
#   ./scripts/run-eval.sh --variants baseline,caching --cases cart_viewmodel
set -euo pipefail
cd "$(dirname "$0")/.."
PY=python3
[ -x .venv/bin/python ] && PY=.venv/bin/python
"$PY" eval/run_eval.py "$@"
echo "Raport: eval/results/report.md"
