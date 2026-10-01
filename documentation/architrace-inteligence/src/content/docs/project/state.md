---
title: Current state
description: Where the work is right now and what the next step is. Read this first when resuming.
---

Hand-over page. It describes only the present: what is in flight, what is decided, what comes
next. History lives in [Progress](../progress/) and in git. Rewrite it before starting a new
task; delete anything that is no longer needed to resume.

Last updated: **2026-10-01**

## Where we are

- Stage 0 (analysis) is done, stage 1 (MVP scope agreement) is waiting for the maintainer.
- PR #20 and PR #21 (foundation documents, this site as the documentation home) are merged.
- Active branch: `ARCHI-22-record-decisions`: ADR 0001–0003 and the decisions recorded on
  the project pages. Nothing else is in flight.
- CI on `main` is red for known reasons (Spotless, agent test compilation, invalid
  `pr-ci.yml`); see [Requirements §3](../requirements/#3-what-exists-today-inventory-of-main-2026-10-01).
- Automation token for GitHub API is issued and verified; git pushes use SSH.

## Decisions

Agreed and recorded (Requirements §7, ADR 0001–0003): PostgreSQL + Liquibase, CodeRabbit,
maintainer-authored PRs with a zero-approval ruleset, sequential `ARCHI-<n>`, copyright holder.

Still open before design starts: **MVP scope** (Requirements §6, question 1) and **UI stack**
(question 2). Spring Boot repository pinning is decided in the control plane design.

## Next step

1. Get `ARCHI-22-record-decisions` merged. Agree the MVP scope and the UI stack, record
   them in Requirements §8.
2. Open the design stage: architecture page, first ADRs (storage, UI stack, module layout,
   versioning), feature pages for M0–M7, feature order.
3. First implementation feature is M0 (engineering platform): repository hygiene, single PR
   pipeline, versioning, `.coderabbit.yaml`, scanners, `main` ruleset. The maintainer installs
   the CodeRabbit app in parallel.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper).
