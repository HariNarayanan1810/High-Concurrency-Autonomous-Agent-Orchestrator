[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $existing = & git config --get core.hooksPath
    if ($LASTEXITCODE -notin @(0, 1)) { throw 'Could not read repository hook configuration.' }
    if ($existing -and $existing -ne '.githooks') {
        throw "Existing hooks path '$existing' was preserved. Review it before installing these hooks."
    }
    foreach ($hookName in @('pre-commit', 'pre-push')) {
        if (-not (Test-Path -LiteralPath (Join-Path $projectRoot ".githooks/$hookName"))) {
            throw "Missing hook $hookName."
        }
    }
    if ($env:OS -ne 'Windows_NT') {
        & chmod +x .githooks/pre-commit .githooks/pre-push
        if ($LASTEXITCODE -ne 0) { throw 'Could not make hooks executable.' }
    }
    & git config --local core.hooksPath .githooks
    if ($LASTEXITCODE -ne 0) { throw 'Could not configure repository hooks.' }
    Write-Output 'Installed repository-local hooks. Other repositories were not changed.'
    Write-Output 'Signing keys and GitHub main protection must also be configured separately.'
} finally {
    Pop-Location
}
