---
title: Current state
description: Where the work is right now and what the next step is. Read this first when resuming.
---

Hand-over page. It describes only the present: what is in flight, what is decided, what comes
next. History lives in [Progress](../progress/) and in git. Rewrite it before starting a new
task; delete anything that is no longer needed to resume.

Last updated: **2026-10-01**

## Where we are

- Stages 0 (analysis), 1 (MVP scope) and 2 (design) are done: architecture, feature pages
  M0–M7 and ADR 0001–0010 are agreed. Stage 3 (implementation) starts with M0.
- PR #20 and PR #21 (foundation documents, this site as the documentation home) are merged.
- PRs #22 (decisions) and #23 (design) are open or merged; check `gh pr list`.
- M0 PR 1 (`ARCHI-24-repository-hygiene`) is open for review: `main` builds green again,
  stale files removed, templates fixed, community files added. The agent coverage gate is a
  temporary ratchet (50 / 28 / 50 %) that M1 must raise back to 85 %.
- Until M0 PR 1 is merged, CI on `main` is red (Spotless, agent test compilation, invalid
  `pr-ci.yml`); the PR fixes all three.
- Automation token for GitHub API is issued and verified; git pushes use SSH.

## Decisions

Agreed and recorded (Requirements §7–8, ADR 0001–0010): PostgreSQL + Liquibase, CodeRabbit,
maintainer-authored PRs with a zero-approval ruleset, React + TypeScript UI, sequential
`ARCHI-<n>`, copyright holder.

MVP scope agreed: M0–M7 (Requirements §8); design agreed on 2026-10-01. No decision is
pending.

## Next step

1. Merge M0 PR 1 (hygiene). Then M0 PR 2: `build-logic` conventions and
   versioning (ARCHI-25), PR 3: pipelines and scanners, PR 4: main pipeline, release, images,
   PR 5: ruleset, repository settings, `.coderabbit.yaml`. Plan: [M0 page](../features/m0-engineering-platform/).
2. The maintainer installs the CodeRabbit app and creates an NVD API key secret (`NVD_API_KEY`)
   before PR 3.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper).
