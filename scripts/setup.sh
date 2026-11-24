#!/usr/bin/env bash
# Stawia środowisko: venv Pythona, build serwera MCP, testy, sprawdzenie narzędzi.
# Użycie: ./scripts/setup.sh
set -euo pipefail
cd "$(dirname "$0")/.."

ok()   { printf '  \033[32m✔\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m!\033[0m %s\n' "$1"; }
need() { command -v "$1" >/dev/null 2>&1 || { echo "Brak: $1 — $2"; exit 1; }; }

echo "▶ Narzędzia"
need python3 "zainstaluj Python 3.10+"
need node "zainstaluj Node.js 20+"
need npm "instaluje się z Node.js"
ok "python3 $(python3 --version | cut -d' ' -f2), node $(node -v)"
command -v claude >/dev/null && ok "Claude Code $(claude --version 2>/dev/null | head -1)" \
  || warn "Claude Code nie znaleziony: npm install -g @anthropic-ai/claude-code"
command -v adb >/dev/null && ok "adb" || warn "adb niedostępny — narzędzia adb w MCP nie zadziałają"
[ -n "${ANTHROPIC_API_KEY:-}" ] && ok "ANTHROPIC_API_KEY ustawiony" \
  || warn "ANTHROPIC_API_KEY nie ustawiony — eval zadziała tylko z --dry-run"

echo "▶ Python (venv + zależności eval)"
python3 -m venv .venv
.venv/bin/pip install -q -r eval/requirements.txt
ok "venv .venv"

echo "▶ Serwer MCP android-dev-mcp"
(cd mcp/android-dev-mcp && npm install --no-audit --no-fund --silent && npm run build --silent && npm test --silent >/dev/null)
ok "zbudowany i przetestowany (mcp/android-dev-mcp/dist)"

echo "▶ Testy bezpieczeństwa i scoringu"
.venv/bin/pytest -q security eval/tests

echo "▶ Hooki Claude Code"
chmod +x security/masking/hook.py
echo '{"hook_event_name":"UserPromptSubmit","prompt":"PESEL 44051401359"}' \
  | CLAUDE_PROJECT_DIR="$PWD" python3 security/masking/hook.py 2>/dev/null \
  && warn "hook NIE zablokował testowego PESEL-u" || ok "hook maskujący działa"

echo
echo "Gotowe. Dalej: ./scripts/run-eval.sh --dry-run   (albo bez flagi, z kluczem API)"
