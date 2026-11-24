param([switch]$DryRun, [string]$Variants = 'all', [string]$Cases = '')
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
$py = if (Test-Path .\.venv\Scripts\python.exe) { '.\.venv\Scripts\python.exe' } else { 'python' }
$argsList = @('eval/run_eval.py', '--variants', $Variants)
if ($Cases) { $argsList += @('--cases', $Cases) }
if ($DryRun) { $argsList += '--dry-run' }
& $py @argsList
Write-Host 'Raport: eval/results/report.md'
