# Stawia środowisko na Windows: venv Pythona, build serwera MCP, testy.
# Użycie (PowerShell 7+): ./scripts/setup.ps1
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

function Ok($m)   { Write-Host "  ✔ $m" -ForegroundColor Green }
function Warn($m) { Write-Host "  ! $m" -ForegroundColor Yellow }
function Need($cmd, $hint) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) { throw "Brak: $cmd — $hint" }
}

Write-Host '▶ Narzędzia'
Need 'python' 'zainstaluj Python 3.10+'
Need 'node' 'zainstaluj Node.js 20+'
Ok "python $(python --version), node $(node -v)"
if (Get-Command claude -ErrorAction SilentlyContinue) { Ok 'Claude Code' } else { Warn 'npm install -g @anthropic-ai/claude-code' }
if (Get-Command adb -ErrorAction SilentlyContinue) { Ok 'adb' } else { Warn 'adb niedostępny' }
if ($env:ANTHROPIC_API_KEY) { Ok 'ANTHROPIC_API_KEY ustawiony' } else { Warn 'brak ANTHROPIC_API_KEY — eval tylko z --dry-run' }

Write-Host '▶ Python'
python -m venv .venv
& .\.venv\Scripts\pip.exe install -q -r eval\requirements.txt
Ok 'venv .venv'

Write-Host '▶ Serwer MCP'
Push-Location mcp\android-dev-mcp
npm install --no-audit --no-fund --silent
npm run build --silent
npm test --silent | Out-Null
Pop-Location
Ok 'android-dev-mcp zbudowany'

Write-Host '▶ Testy'
& .\.venv\Scripts\pytest.exe -q security eval\tests

Write-Host "`nGotowe. Dalej: ./scripts/run-eval.ps1 -DryRun"
