---
title: 0003. Maintainer-authored pull requests and the merge gate
description: All pull requests are authored by the maintainer; the merge is the approval; gates come from checks and resolved threads.
---

Status: accepted
Date: 2026-10-01

## Context

All work in this repository is committed and submitted as pull requests from the maintainer's
own account; no other identity appears in commits, PRs or documentation. GitHub does not allow
an author to approve their own pull request, so a rule that requires one approving review would
make every pull request unmergeable. A gate is still required: nothing reaches `main` without
green checks and a finished review.

## Decision

- Pull requests stay authored by the maintainer's account. No bot or machine user is introduced.
- The `main` branch ruleset requires: a pull request, **zero** approving reviews, all required
  status checks green, all review conversations resolved, linear history (squash merge only),
  no force pushes, no deletions, no bypass actors.
- The maintainer's merge action is the approval. The review record is the resolved CodeRabbit
  threads plus the PR description.

## Consequences

- Authorship and attribution remain exactly as the maintainer wants them.
- `delete_branch_on_merge` is enabled so merged branches disappear; follow-up work always
  goes to a new branch and a new pull request, never onto a merged branch.
- If a second human maintainer joins, the ruleset is raised to one required approval and this
  record is superseded.

## Alternatives considered

- **Bot identity (GitHub App or machine user) opening the pull requests** so the maintainer
  can formally approve: rejected because commits and pull requests would carry a second
  identity.
- **No ruleset at all**: rejected; the whole point of the process is that gates are enforced
  by the platform, not by discipline.
