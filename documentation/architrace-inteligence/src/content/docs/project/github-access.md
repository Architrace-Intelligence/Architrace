---
title: GitHub setup
description: Repository access, automation permissions and the maintainer checklist.
---

What the automation needs to operate the repository, what it already has, and what only the
maintainer can do. Written for `Architrace-Intelligence/Architrace`. Companion of [Requirements](../requirements/).

## 1. Current state (observed 2026-10-01)

| Item                                   | Value                                                                 |
|----------------------------------------|-----------------------------------------------------------------------|
| Repository                             | public, default branch `main`, issues on, wiki off                    |
| Maintainer role                        | `godmarch33`: admin on the repo, owner of the organisation            |
| CLI token scopes (`gh auth status`)    | `repo`, `workflow`, `read:org`, `gist`                                |
| Branch protection                      | none active; ruleset `main-rule` exists but is **disabled**           |
| Merge methods                          | merge, squash, rebase all allowed; auto-merge off; branch deletion off|
| Actions                                | enabled, all actions allowed, default `GITHUB_TOKEN` permission: read |
| Secrets                                | `SONAR_TOKEN`, `SNYK_TOKEN`, `PAGES_DEPLOY_TOKEN`                     |
| Variables                              | `SNYK_ORG=architect-intelligence`                                     |
| GitHub Apps installed on the org       | SonarQube Cloud, Snyk                                                 |
| Security features                      | Dependabot security updates, secret scanning: all disabled            |
| Labels                                 | GitHub defaults only (the custom labels from `LABELS.md` were never created) |

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

## 5. Proposed `main` ruleset

- Restrict deletions, block force pushes, require linear history.
- Require a pull request before merging; squash merge only; `delete_branch_on_merge` on.
- Required status checks: `build`, `quality`, `security`, `docs`, `title` and the CodeQL analyses
  (the job names of `pr.yml`, `pr-title.yml` and `codeql.yml`). The AI review gate is *required
  conversation resolution*, not a status check.
- Require conversation resolution before merge.
- No bypass actors.

## 6. Setup checklist for the maintainer

- [x] Decide §4 (review identity): option A.
- [x] Choose the AI reviewer: CodeRabbit ([ADR 0002](../adr/0002-coderabbit-ai-review/)).
- [ ] Install the CodeRabbit GitHub App on `Architrace-Intelligence/Architrace` (organisation owner, UI only).
- [ ] Confirm SonarCloud project key: the build still uses `Architrace-Intelligence_Architrace-agent`;
      the project in SonarCloud must have *Automatic Analysis* switched off because analysis is
      CI-driven.
- [ ] Confirm the Snyk organisation slug (`SNYK_ORG`).
- [ ] Request an [NVD API key](https://nvd.nist.gov/developers/request-an-api-key) and store it as
      the repository secret `NVD_API_KEY`; OWASP Dependency-Check is skipped until it exists.
- [ ] Add `NVD_API_KEY` and `SNYK_TOKEN` as Dependabot secrets too (Settings → Secrets and
      variables → Dependabot), so Dependabot pull requests get the same scanners.

- [x] Ticket numbering: `ARCHI-<n>` sequential.
- [x] Copyright holder: `Dmytro Hryshchenko`; header fixed in the hygiene feature.

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
