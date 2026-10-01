---
title: Current state
description: Where the work is right now and what the next step is. Read this first when resuming.
---

Hand-over page. It describes only the present: what is in flight, what is decided, what comes
next. History lives in [Progress](../progress/) and in git. Rewrite it before starting a new
task; delete anything that is no longer needed to resume.

Last updated: **2026-10-01**

## Where we are

- Stages 0–2 are done. Stage 3 (implementation) is open: M0 PR 1 (hygiene, ARCHI-24) is merged
  as #26 and `main` builds green. The agent coverage gate is a temporary ratchet
  (50 / 28 / 50 %) that M1 must raise back to 85 %.
- The UI design (ARCHI-25) is in review: eight interactive prototypes plus the canvas
  `MVP-frames` (five static frames of the initial release: Projects list and Service map) in
  the Claude Design project "Architrace UI", the [UI design](../features/ui-design/) page,
  and the sources under `design/ui-prototype/` and `design/ui-frames/`. The visual direction
  (dark-first "calm control room") was chosen without a brief and needs the maintainer's
  confirmation or redirection.
- Automation token for the GitHub API is issued and verified; git pushes use SSH.

## Decisions

Agreed and recorded (Requirements §7–8, ADR 0001–0010): PostgreSQL + Liquibase, CodeRabbit,
maintainer-authored PRs with a zero-approval ruleset, React + TypeScript UI, sequential
`ARCHI-<n>`, copyright holder. MVP scope M0–M7; design agreed on 2026-10-01.

The initial UI release is narrowed to two screens (maintainer, 2026-10-01): a Projects list
filtered by project, environment and cluster, and the Service map of the chosen scope with
its services and data streams.

Pending: confirmation of the UI direction, the source of the *project* grouping (resource
attribute or agent setting), and the drift refinement proposed on the UI design page
(compare deployments in timeline mode only).

## Next step

1. Maintainer reviews ARCHI-25 (UI design), starting with the `MVP-frames` canvas; open
   points are listed at the end of the [UI design](../features/ui-design/) page. Changes go
   into the same PR until it is merged.
2. Then M0 continues with the next ticket number: PR 2 `build-logic` conventions and
   versioning, PR 3 pipelines and scanners, PR 4 main pipeline, release, images, PR 5 ruleset,
   repository settings, `.coderabbit.yaml`. Plan: [M0 page](../features/m0-engineering-platform/).
3. The maintainer installs the CodeRabbit app and creates an NVD API key secret (`NVD_API_KEY`)
   before PR 3.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper).
