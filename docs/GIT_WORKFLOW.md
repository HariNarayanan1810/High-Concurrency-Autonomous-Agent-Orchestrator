# Git and Pull Request Workflow

Updated: 2026-10-18. Applies to all four members without changing module ownership.

## Daily Workflow

Start with a clean working tree, then update main and create a focused branch:

```powershell
git status
git switch main
git pull --ff-only origin main
git switch -c feature/your-task
```

Use feature/, fix/, docs/, chore/, or test/ prefixes. Do not develop directly on main. Each member signs their own actual contributions; never backdate or manufacture missing daily commits.

Install the versioned hooks once per clone:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Install-GitHooks.ps1
```

This sets only this repository's core.hooksPath. The execution-policy flag applies to this process, not the system. Existing custom hook paths are preserved and reported rather than overwritten. On non-Windows systems, run the script with pwsh.

Before committing:

```powershell
mvn -B -ntp clean verify
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Test-GitHooks.ps1
git add .
git diff --cached --stat
git commit -S -m "Describe the actual change"
git push -u origin feature/your-task
```

Hooks block commits on main/detached/nonstandard branches, missing signing configuration, staged whitespace errors, direct pushes to main, and new outgoing commits without a signature header. They do not run Maven automatically.

The pre-push check detects signature presence, not cryptographic validity. Hooks can be bypassed or edited, so they are not a security boundary. GitHub must verify signatures and enforce protected main. Previously published main history is not rewritten to retroactively change unsigned commits.

## Signing-Key Verification

Hari's repository already uses SSH signing. No new private key is needed. GitHub's October 18 API check returned unknown_key for 13a2e75 and zero signing keys for HariNarayanan1810, despite the user reporting a signing-key entry. Check the account username and key type before treating this as resolved.

From the correct account, open https://github.com/settings/keys and register the existing public key with type Signing Key:

```powershell
Get-Content "$env:USERPROFILE\.ssh\zaalima_signing_ed25519.pub" | Set-Clipboard
```

Only the .pub key is uploaded; the private key and its passphrase remain local. An Authentication Key entry alone does not register a Signing Key. Commit email must be verified on the same GitHub account. Check the actual commit's Verified badge after registration; do not assume success from a successful push.

## Required GitHub Settings

The October 18 API check reported main protected=false. Repository owner must configure Settings -> Branches -> branch protection (or an equivalent active ruleset) for main:

- Require a pull request before merging and one teammate approval.
- Dismiss stale approvals after changes.
- Require signed commits.
- Require conversation resolution and passing status checks.
- Select Java 21 Build and Verified PR Commits after the workflow has run once.
- Apply rules to administrators / disallow bypassing.
- Keep force pushes and deletion disabled.

Require teammate review for the lead's PR too. If a check does not appear yet, first push a branch and open the PR, wait for its workflow, then select the check. These repository settings are not enabled by adding this document or workflow file.

## CI and Review

Backend CI builds/packages Java 21 and runs Maven tests plus the hook tests on pull requests into main and pushes to main. A separate PR job checks GitHub's verified flag for PR commits. It uses read-only permissions and does not execute PR code with privileged pull_request_target access. Official actions are pinned to full commit IDs.

The workflow is supplied locally; a GitHub Actions run cannot happen until it is pushed. Workflow/check results and main protection must be verified on GitHub before marking enforcement complete. The workflow itself is editable in a PR; protected-branch rules are the authority for verified signatures and approvals.

Open a PR with base main and compare your branch. Include actual test results and pending dependencies. After review/checks pass, merge on GitHub and update local main with git pull --ff-only origin main.

## References

- https://git-scm.com/docs/githooks
- https://docs.github.com/en/authentication/managing-commit-signature-verification/telling-git-about-your-signing-key
- https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches
