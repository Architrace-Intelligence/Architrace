---
title: Current state
description: Where the work is right now and what the next step is. Read this first when resuming.
---

Hand-over page. It describes only the present: what is in flight, what is decided, what comes
next. History lives in [Progress](../progress/) and in git. Rewrite it before starting a new
task; delete anything that is no longer needed to resume.

Last updated: **2026-10-01**

## Where we are

- Stages 0–2 are done. Stage 3 (implementation) is open. M0 PR 1 (hygiene, ARCHI-24) is merged
  as #26; the remaining M0 PRs (build-logic, pipelines, release, ruleset) are deferred behind
  the feature slices the maintainer asked for on 2026-10-01: the Projects list and the Service
  map with their backend, step by step.
- The UI design (ARCHI-25) is merged as #30: eight interactive prototypes plus the canvas
  `MVP-frames` (initial release: Projects list and Service map) in the Claude Design project
  "Architrace UI", the [UI design](../features/ui-design/) page, sources under `design/`.
  The visual direction (dark-first "calm control room") was chosen without a brief and still
  needs the maintainer's confirmation or redirection.
- M2 is complete once ARCHI-29 merges: PostgreSQL schema and Spring Data JDBC stores
  (ARCHI-26, #31), ingestion over the extended gRPC contract with liveness and metrics
  (ARCHI-28, #33), and `TopologyQuery` (current graph per scope at time T, `ScopeSummary` per
  scope), `RetentionJob` and Actuator health and metrics (ARCHI-29, in review). Every module
  carries a `gradle.lockfile` (ARCHI-27, #32); a dependency change must rewrite it with
  `--write-locks`. The agent coverage ratchet is 52 / 31 / 50 % until M1 restores 85 %.
- Automation token for the GitHub API is issued and verified; git pushes use SSH.

## Decisions

Agreed and recorded (Requirements §7–8, ADR 0001–0010): PostgreSQL + Liquibase, CodeRabbit,
maintainer-authored PRs with a zero-approval ruleset, React + TypeScript UI, sequential
`ARCHI-<n>`, copyright holder. MVP scope M0–M7; design agreed on 2026-10-01.

The initial UI release is narrowed to two screens (maintainer, 2026-10-01): a Projects list
filtered by project, environment and cluster, and the Service map of the chosen scope with
its services and data streams.

Working assumption since ARCHI-26: a **scope** is project × environment × cluster, reported by
the agent at registration and stored on every snapshot. Pending: where the project value comes
from on the agent side (resource attribute or agent setting), confirmation of the UI direction,
and the drift refinement proposed on the UI design page (compare deployments in timeline mode
only).

## Next step

M2 PR 3 (ARCHI-29) is open for review. Merge it first (one PR at a time), then rebase and
update this page.

Vertical slices towards the two screens, one PR each, in this order:

1. ARCHI-30: OpenAPI document and generator, scopes and agents endpoints on top of
   `TopologyQuery.scopes()` and `AgentStore`, problem details (M3 PR 1); ARCHI-31: graph and
   services endpoints on top of `TopologyQuery.currentGraph` (M3 PR 2).
2. ARCHI-32: `architrace-ui` scaffold served from the control plane jar (M4 PR 1);
   ARCHI-33: Projects list page; ARCHI-34: Service map page; ARCHI-35: lenses, URL state, polish.
3. M1 (agent pipeline) follows so the demo stack feeds real data; until then a fixture loader
   seeds snapshots for development.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper).
