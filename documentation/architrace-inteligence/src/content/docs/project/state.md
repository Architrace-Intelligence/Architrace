---
title: Current state
description: Where the work is right now and what the next step is. Read this first when resuming.
---

Hand-over page. It describes only the present: what is in flight, what is decided, what comes
next. History lives in [Progress](../progress/) and in git. Rewrite it before starting a new
task; delete anything that is no longer needed to resume.

Last updated: **2026-10-01**

## Where we are

- Stages 0 (analysis) and 1 (MVP scope agreement) are done. Stage 2 (design) is in review.
- PR #20 and PR #21 (foundation documents, this site as the documentation home) are merged.
- `ARCHI-22-record-decisions` (ADR 0001–0004, agreed scope) is open or merged; check.
- Active branch: `ARCHI-23-design`: architecture page, feature pages M0–M7, ADR 0005–0010
  (proposed), mermaid rendering on the site. Nothing else is in flight.
- CI on `main` is red for known reasons (Spotless, agent test compilation, invalid
  `pr-ci.yml`); see [Requirements §3](../requirements/#3-what-exists-today-inventory-of-main-2026-10-01).
- Automation token for GitHub API is issued and verified; git pushes use SSH.

## Decisions

Agreed and recorded (Requirements §7, ADR 0001–0004): PostgreSQL + Liquibase, CodeRabbit,
maintainer-authored PRs with a zero-approval ruleset, React + TypeScript UI, sequential
`ARCHI-<n>`, copyright holder.

MVP scope agreed: M0–M7 (Requirements §8). Spring Boot repository pinning is decided in the
control plane design.

## Next step

1. Get the design PR reviewed: the maintainer agrees or amends the feature pages and
   ADR 0005–0010; on agreement flip the ADR status to accepted (one follow-up commit).
2. Start M0 (engineering platform) following its delivery plan, PR by PR, starting with the
   hygiene PR that makes `main` green. Next ticket number: ARCHI-24.
3. The maintainer installs the CodeRabbit app in parallel.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper).
