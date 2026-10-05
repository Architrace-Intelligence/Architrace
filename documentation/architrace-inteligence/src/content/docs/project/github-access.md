---
title: GitHub setup
description: Repository access, automation permissions and the maintainer checklist.
---

What the automation needs to operate the repository, what it already has, and what only the
maintainer can do. Written for `Architrace-Intelligence/Architrace`. Companion of [Requirements](../requirements/).

## 1. Current state (observed 2026-10-04)

| Item                                   | Value                                                                 |
|----------------------------------------|-----------------------------------------------------------------------|
| Repository                             | public, default branch `main`, issues on, wiki off                    |
| Maintainer role                        | `godmarch33`: admin on the repo, owner of the organisation            |
| CLI token scopes (`gh auth status`)    | `repo`, `workflow`, `read:org`, `gist`                                |
| Branch protection                      | ruleset `main-rule` (id `13328234`) exists but is **disabled**; the agreed definition is `.github/rulesets/main.json`, see §5 |
| Merge methods                          | merge, squash, rebase all allowed; auto-merge off; branch deletion off; target in §5 |
| Actions                                | enabled, all actions allowed, default `GITHUB_TOKEN` permission: read |
| Secrets                                | `SONAR_TOKEN`, `SNYK_TOKEN`, `NVD_API_KEY`, `PAGES_DEPLOY_TOKEN`; the scanner secrets also exist as Dependabot secrets |
| Variables                              | `SNYK_ORG=architrace`                                                 |
| GitHub Apps installed on the org       | SonarQube Cloud, Snyk; CodeRabbit not yet                             |
| Security features                      | Dependabot alerts on; Dependabot security updates, secret scanning and push protection off; CodeQL through `codeql.yml` |
| Labels                                 | the set from `LABELS.md`, created in the hygiene PR (#26)             |

## 2. What the current token already allows

With `repo` + `workflow` and admin on the repository the automation can:

- create branches, push, open / update / close pull requests, comment and request reviews
- create, change and delete workflow files under `.github/workflows`
- manage repository secrets and variables
- create and enable branch rulesets, require status checks and reviews
- create labels, milestones, issues, releases and tags
- enable Dependabot alerts, Dependabot security updates, secret scanning and CodeQL default
  setup through the repository API
- configure GitHub Pages

Everything in the CI/CD plan can therefore be done with the present token. No new token is
required for the pipelines themselves.

## 3. What the token cannot do

| Action                                           | Why                                   | Who does it            |
|--------------------------------------------------|---------------------------------------|------------------------|
| Install a GitHub App (CodeRabbit, Gemini, …)     | UI-only, organisation owner           | maintainer             |
| Change organisation-level settings or rulesets   | needs `admin:org` scope               | maintainer             |
| Push container images to GHCR from a laptop      | needs `write:packages`; CI uses its own `GITHUB_TOKEN` | not needed |
| Approve a pull request                           | GitHub forbids approving your own PR  | see §4                 |
| Write repository settings or rulesets from an agent session | the agent's tool permissions stop at administration writes | maintainer, see §5 |

## 4. The review identity problem

All PRs are opened from the maintainer's account, as requested. GitHub does not allow an
author to approve their own pull request, so a rule "1 approving review required" would make
every PR unmergeable.

Options:

| Option | Description | Consequence |
|--------|-------------|-------------|
| **A (recommended)** | Ruleset requires: PR, all status checks green, AI review threads resolved, linear history, **0 approvals**. The maintainer's merge click is the approval. | Authorship stays with the maintainer. No extra accounts. |
| B | Create a GitHub App or a machine user used by the automation to push and open PRs. Maintainer approves as themselves. | Commits and PRs are authored by the bot identity, which contradicts the "only the maintainer is visible" rule. |

**Decided 2026-10-01: option A.** See [ADR 0003](../adr/0003-review-identity-and-merge-gate/).

## 5. The `main` ruleset and the repository settings

The ruleset is kept as code in `.github/rulesets/main.json` (ARCHI-37) and reviewed like any
other change. It is the request body of the rulesets API and the format of the *Import a
ruleset* button under *Settings → Rules → Rulesets*. It encodes
[ADR 0003](../adr/0003-review-identity-and-merge-gate/):

- target: the default branch; enforcement `active`; no bypass actors.
- `deletion`, `non_fast_forward`, `required_linear_history`.
- `pull_request`: **0** approving reviews, required conversation resolution (the CodeRabbit
  gate), squash as the only merge method.
- `required_status_checks`: `build`, `quality`, `security`, `docs`, `title`, `size`,
  `codeql / java-kotlin`, `codeql / javascript-typescript`, `codeql / actions`, each bound to
  the GitHub Actions app (`integration_id` 15368) so no other app can satisfy them. A job that is
  skipped (for example `quality` on Dependabot pull requests, which cannot read the SonarCloud
  token) counts as passed. Branches do not have to be up to date with `main`
  (`strict_required_status_checks_policy: false`): work lands one pull request at a time, and
  the main pipeline runs the same gates on the merge commit.
- `code_scanning`: CodeQL results with no new alert of severity error and no new security alert
  of high or higher severity.

The maintainer applies it with the fine-grained token or the CLI token (both carry
administration rights), either by importing the file in the UI or by updating the existing
ruleset in place:

```bash
gh api -X PUT repos/Architrace-Intelligence/Architrace/rulesets/13328234 --input .github/rulesets/main.json
```

The repository settings that complete the gate are not part of a ruleset:

```bash
gh api -X PATCH repos/Architrace-Intelligence/Architrace --input - <<'JSON'
{
  "allow_squash_merge": true,
  "allow_merge_commit": false,
  "allow_rebase_merge": false,
  "delete_branch_on_merge": true,
  "squash_merge_commit_title": "PR_TITLE",
  "squash_merge_commit_message": "COMMIT_MESSAGES",
  "security_and_analysis": {
    "secret_scanning": { "status": "enabled" },
    "secret_scanning_push_protection": { "status": "enabled" },
    "dependabot_security_updates": { "status": "enabled" }
  }
}
JSON
```

`PR_TITLE` makes the pull request title the subject of the squash commit, which is what the
versioning parses; `COMMIT_MESSAGES` keeps the squashed commit messages in the body, so a
`BREAKING CHANGE:` footer written in any commit survives the squash.

## 6. Setup checklist for the maintainer

- [x] Decide §4 (review identity): option A.
- [x] Choose the AI reviewer: CodeRabbit ([ADR 0002](../adr/0002-coderabbit-ai-review/)).
- [x] Snyk organisation slug: `SNYK_ORG=architrace` (corrected on 2026-10-02).
- [x] `SNYK_TOKEN` rotated on 2026-10-02: Snyk PAT `architrace-ci`, expires 2026-12-31 (Snyk PATs
      live 90 days at most; service accounts are a paid feature). Rotate before it expires.
- [x] `NVD_API_KEY` stored on 2026-10-02 (the key must be activated through the link in the NVD
      e-mail before it works).
- [x] Both secrets mirrored as Dependabot secrets, so Dependabot pull requests get the same scanners.
- [x] Ticket numbering: `ARCHI-<n>` sequential.
- [x] Copyright holder: `Dmytro Hryshchenko`; header fixed in the hygiene feature.
- [ ] Apply the `main` ruleset and the repository settings from §5 (after ARCHI-37 is merged);
      re-apply the ruleset after ARCHI-51, which adds the required check `size` (at most 12
      changed files per pull request), and after ARCHI-52, which renames the CodeQL checks to
      `codeql / <language>`.
- [ ] Install the CodeRabbit GitHub App on `Architrace-Intelligence/Architrace` (organisation
      owner, UI only); `.coderabbit.yaml` is already in the repository.
- [ ] Seed the first release tag on the merge commit of the main pipeline:
      `git tag -a v0.1.0 -m v0.1.0 <commit> && git push origin v0.1.0`. Every later `feat` or
      `fix` merge releases automatically ([ADR 0006](../adr/0006-versioning-and-release-flow/)).
- [ ] Make the two GHCR packages (`architrace-agent`, `architrace-control-plane`) public once;
      they are created private by the first `main` run.
- [ ] Close Dependabot pull request #45: it edits the removed `ci-cd.yml`.
- [ ] Confirm the SonarCloud project key: the build still uses `Architrace-Intelligence_Architrace-agent`;
      the project in SonarCloud must have *Automatic Analysis* switched off because analysis is
      CI-driven.

## 7. Fine-grained token for automation

A fine-grained personal access token, owned by the maintainer, is used by the automation for
API operations (the CLI token in §1 stays untouched; git pushes keep using SSH). The token is
stored outside the repository with owner-only file permissions and is never committed.

Verified on 2026-10-01: a token with the settings below passed every read and write probe
(contents, pull requests, issues, actions, administration, secrets, variables, pages,
environments). The maintainer issued it with a 30-day expiry (2026-10-31); rotate before then.

Organisation policy: fine-grained tokens with a lifetime over **366 days are rejected** by the
organisation, so the token must expire within a year and be rotated.

Token settings:

- Resource owner: `Architrace-Intelligence`
- Repository access: only `Architrace-Intelligence/Architrace`
- Expiration: at most 366 days

| Repository permission   | Level          | Needed for                                                        |
|-------------------------|----------------|-------------------------------------------------------------------|
| Metadata                | Read           | mandatory                                                         |
| Contents                | Read and write | push branches, tags, releases, merge                              |
| Pull requests           | Read and write | open, update, close, merge PRs, review comments                   |
| Issues                  | Read and write | issues, labels, milestones                                        |
| Workflows               | Read and write | create and change files under `.github/workflows`                 |
| Actions                 | Read and write | read logs, re-run and cancel runs                                 |
| Administration          | Read and write | rulesets, merge settings, Dependabot / secret scanning / CodeQL setup |
| Secrets                 | Read and write | repository secrets for scanners and reviewers                     |
| Variables               | Read and write | repository variables                                              |
| Environments            | Read and write | release environments, if used                                     |
| Pages                   | Read and write | documentation site settings                                       |
| Checks                  | Read           | see check runs on PRs                                             |
| Commit statuses         | Read           | see statuses on PRs                                               |
| Code scanning alerts    | Read           | read CodeQL findings                                              |
| Dependabot alerts       | Read           | read dependency findings                                          |
| Secret scanning alerts  | Read           | read secret findings                                              |

No organisation permissions are required.
