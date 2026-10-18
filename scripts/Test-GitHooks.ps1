[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$fixtureParent = [IO.Path]::GetFullPath((Join-Path $projectRoot 'target/hook-tests'))
$fixtureRoot = Join-Path $fixtureParent ([Guid]::NewGuid().ToString('N'))
$fixtureRepo = Join-Path $fixtureRoot 'repository'
$checks = 0
$savedGlobalConfig = $env:GIT_CONFIG_GLOBAL
$savedSystemConfig = $env:GIT_CONFIG_NOSYSTEM

function Invoke-FixtureGit {
    param([string[]]$Arguments, [int]$ExpectedExit = 0)
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & git @Arguments 2>&1
        $code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($code -ne $ExpectedExit) {
        throw "Fixture git expected exit $ExpectedExit, got ${code}: $output"
    }
    return ($output | ForEach-Object { $_.ToString() }) -join "`n"
}

function Invoke-PushHook {
    param([string]$InputLine)
    Push-Location $fixtureRepo
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = $InputLine | & $shellCommand $pushHook origin fixture-only 2>&1
        return [pscustomobject]@{ Code = $LASTEXITCODE; Text = ($output | Out-String) }
    } finally {
        $ErrorActionPreference = $previousPreference
        Pop-Location
    }
}

try {
    New-Item -ItemType Directory -Path $fixtureRoot -Force | Out-Null
    $emptyConfig = Join-Path $fixtureRoot 'empty.gitconfig'
    [IO.File]::WriteAllText($emptyConfig, '')
    $env:GIT_CONFIG_GLOBAL = $emptyConfig
    $env:GIT_CONFIG_NOSYSTEM = '1'
    if ($env:OS -eq 'Windows_NT') {
        $gitDirectory = Split-Path -Parent (Get-Command git).Source
        $shellCommand = (Resolve-Path (Join-Path $gitDirectory '../bin/sh.exe')).Path
    } else {
        $shellCommand = (Get-Command sh).Source
    }
    $pushHook = (Join-Path $projectRoot '.githooks/pre-push').Replace('\', '/')
    $hookDirectory = (Join-Path $projectRoot '.githooks').Replace('\', '/')
    if ($env:OS -ne 'Windows_NT') {
        & chmod +x (Join-Path $projectRoot '.githooks/pre-commit') $pushHook
        if ($LASTEXITCODE -ne 0) { throw 'Could not make test hooks executable.' }
    }

    Invoke-FixtureGit -Arguments @('init', '-b', 'main', $fixtureRepo) | Out-Null
    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'config', 'user.name', 'Hook Test Fixture') | Out-Null
    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'config', 'user.email', 'hooks@example.invalid') | Out-Null
    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, '-c', 'commit.gpgsign=false', 'commit', '--allow-empty', '-m', 'Fixture baseline') | Out-Null
    $baseline = Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'rev-parse', 'HEAD')
    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'update-ref', 'refs/remotes/origin/main', $baseline) | Out-Null
    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'config', 'core.hooksPath', $hookDirectory) | Out-Null
    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'config', 'commit.gpgsign', 'true') | Out-Null
    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'config', 'user.signingkey', 'fixture-placeholder') | Out-Null
    $rejected = Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'commit', '--allow-empty', '-m', 'Main must be blocked') -ExpectedExit 1
    if ($rejected -notmatch 'Use feature/') { throw 'Main commit failed for an unexpected reason.' }
    $checks++

    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'switch', '-c', 'feature/hook-test') | Out-Null
    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'config', 'commit.gpgsign', 'false') | Out-Null
    $rejected = Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'commit', '--allow-empty', '-m', 'Disabled signing must be blocked') -ExpectedExit 1
    if ($rejected -notmatch 'commit.gpgsign=true') { throw 'Signing-disabled commit failed for an unexpected reason.' }
    $checks++

    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'config', 'commit.gpgsign', 'true') | Out-Null
    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'config', '--unset', 'user.signingkey') | Out-Null
    $rejected = Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'commit', '--allow-empty', '-m', 'Missing signing key must be blocked') -ExpectedExit 1
    if ($rejected -notmatch 'Configure user.signingkey') { throw 'Missing-key commit failed for an unexpected reason.' }
    $checks++

    # This disposable key/identity belongs only to these tests, never to a team member.
    $fixtureKey = Join-Path $fixtureRoot 'temporary_signing_key'
    if ($env:OS -eq 'Windows_NT') {
        $keygen = Start-Process -FilePath (Get-Command ssh-keygen).Source -ArgumentList @('-q', '-t', 'ed25519', '-N', '""', '-f', ('"' + $fixtureKey + '"')) -WindowStyle Hidden -Wait -PassThru
        if ($keygen.ExitCode -ne 0) { throw 'Could not create disposable signing key.' }
    } else {
        & ssh-keygen -q -t ed25519 -N '' -f $fixtureKey
        if ($LASTEXITCODE -ne 0) { throw 'Could not create disposable signing key.' }
    }
    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'config', 'gpg.format', 'ssh') | Out-Null
    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'config', 'user.signingkey', $fixtureKey) | Out-Null
    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'config', 'gpg.ssh.program', (Get-Command ssh-keygen).Source) | Out-Null
    [IO.File]::WriteAllText((Join-Path $fixtureRepo 'whitespace.txt'), "fixture with trailing space `n")
    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'add', 'whitespace.txt') | Out-Null
    $rejected = Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'commit', '-m', 'Whitespace must be blocked') -ExpectedExit 1
    if ($rejected -notmatch 'trailing whitespace') { throw 'Staged whitespace was not detected.' }
    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'reset', '--', 'whitespace.txt') | Out-Null
    $checks++

    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'commit', '--allow-empty', '-m', 'Signed fixture commit') | Out-Null
    $signed = Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'rev-parse', 'HEAD')
    $checks++

    $zero = '0' * 40
    $result = Invoke-PushHook "refs/heads/feature/hook-test $signed refs/heads/main $baseline"
    if ($result.Code -eq 0 -or $result.Text -notmatch 'Direct pushes') { throw "Main push not rejected: $($result.Text)" }
    $checks++

    $result = Invoke-PushHook "refs/heads/feature/hook-test $signed refs/heads/feature/hook-test $zero"
    if ($result.Code -ne 0) { throw "Signed feature push rejected: $($result.Text)" }
    $checks++

    Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, '-c', 'core.hooksPath=', 'commit', '--allow-empty', '--no-gpg-sign', '-m', 'Unsigned fixture for rejection test') | Out-Null
    $unsigned = Invoke-FixtureGit -Arguments @('-C', $fixtureRepo, 'rev-parse', 'HEAD')
    $result = Invoke-PushHook "refs/heads/feature/hook-test $unsigned refs/heads/feature/hook-test $signed"
    if ($result.Code -eq 0 -or $result.Text -notmatch 'Unsigned outgoing commit') { throw "Unsigned push not rejected: $($result.Text)" }
    $checks++

    $result = Invoke-PushHook "refs/heads/feature/hook-test $signed refs/heads/random-name $zero"
    if ($result.Code -eq 0) { throw 'Nonstandard branch push was not rejected.' }
    $checks++

    Write-Output "Git hook checks passed: $checks. No network, user key, or actual repository commit was used."
} finally {
    $env:GIT_CONFIG_GLOBAL = $savedGlobalConfig
    $env:GIT_CONFIG_NOSYSTEM = $savedSystemConfig
    $resolvedFixture = [IO.Path]::GetFullPath($fixtureRoot)
    $requiredPrefix = $fixtureParent.TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    if (-not $resolvedFixture.StartsWith($requiredPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Refusing to clean a fixture outside the intended hook-tests directory.'
    }
    if (Test-Path -LiteralPath $resolvedFixture) {
        Remove-Item -LiteralPath $resolvedFixture -Recurse -Force
    }
}
