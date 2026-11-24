#!/usr/bin/env bash
# Tworzy gałąź demo/bad-code z celowo wadliwym ViewModelem — materiał do pokazania agenta review.
#   ./scripts/create-demo-pr.sh
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -n "$(git status --porcelain)" ]; then
  echo "Najpierw zacommituj lub schowaj zmiany (git status nie jest czysty)."; exit 1
fi

BASE=$(git rev-parse --abbrev-ref HEAD)
git switch -c demo/bad-code
mkdir -p app/src/main/kotlin/com/example/claims/ui/demo
cp docs/demo/LegacyClaimViewModel.kt app/src/main/kotlin/com/example/claims/ui/demo/
git add app/src/main/kotlin/com/example/claims/ui/demo/LegacyClaimViewModel.kt
git commit -m "Demo: legacy claim ViewModel (celowo wadliwy, do review AI)"

cat <<MSG

Gałąź demo/bad-code gotowa (baza: $BASE).

Review LOKALNIE — w ramach abonamentu Claude, bez kosztów API:
  claude "Użyj agenta android-reviewer, żeby przejrzeć zmiany względem $BASE"

Review w CI (płatne Claude API — tylko świadomie):
  git push -u origin demo/bad-code
  otwórz PR i dodaj etykietę "ai-review"

Powrót: git switch $BASE
MSG
