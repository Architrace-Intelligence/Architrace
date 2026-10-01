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
- PR #20 (AGENTS.md, requirements, progress, GitHub setup, PR template) is merged.
- Active branch: `ARCHI-21-docs-site`: moves the project pages onto this site, adds this page
  and the contributing page, documents the automation token. Nothing else is in flight.
- CI on `main` is red for known reasons (Spotless, agent test compilation, invalid
  `pr-ci.yml`); see [Requirements §3](../requirements/#3-what-exists-today-inventory-of-main-2026-10-01).
- Automation token for GitHub API is issued and verified; git pushes use SSH.

## Decisions pending

Listed in [Requirements §6, open questions](../requirements/#open-questions-for-the-maintainer)
and in [GitHub setup §6](../github-access/#6-setup-checklist-for-the-maintainer). Until they are
answered no design or implementation work starts.

## Next step

1. Get `ARCHI-21-docs-site` merged. Receive the maintainer's answers, record them in
   Requirements §7 ("Agreed MVP") and in the GitHub setup checklist.
2. Open the design stage: architecture page, first ADRs (storage, UI stack, module layout,
   versioning), feature pages for M0–M7, feature order.
3. First implementation feature is M0 (engineering platform): repository hygiene, single PR
   pipeline, versioning, AI review, scanners, `main` ruleset.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper).
