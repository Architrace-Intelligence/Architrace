# GitHub access and repository setup

What the automation needs to operate the repository, what it already has, and what only the
maintainer can do. Written for `Architrace-Intelligence/Architrace`.

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

Decision pending from the maintainer. Option A is assumed in the CI/CD plan.

## 5. Proposed `main` ruleset

- Restrict deletions, block force pushes, require linear history.
- Require a pull request before merging; squash merge only; `delete_branch_on_merge` on.
- Required status checks: `build`, `quality`, `security`, `ai-review` (names fixed in the CI
  design).
- Require conversation resolution before merge.
- No bypass actors.

## 6. Setup checklist for the maintainer

- [ ] Decide §4 (review identity).
- [ ] Choose the AI reviewer (requirements §5.4) and install it if it is a GitHub App.
- [ ] Confirm SonarCloud project key: the build still uses `Architrace-Intelligence_Architrace-agent`;
      the project in SonarCloud must have *Automatic Analysis* switched off because analysis is
      CI-driven.
- [ ] Confirm the Snyk organisation slug (`SNYK_ORG`).
- [ ] If an LLM-backed reviewer is chosen, add its API key as a repository secret.
- [ ] Provide the ticket numbering source (`ARCHI-<n>`) for branch and commit naming.
- [ ] Confirm the copyright holder spelling in `license-header.txt` ("Dmitry Hryshchenko").
