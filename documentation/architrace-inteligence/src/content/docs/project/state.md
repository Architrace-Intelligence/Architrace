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
- M2 PR 1 (ARCHI-26) is delivered: control plane on Maven Central only, PostgreSQL schema
  through Liquibase, Spring Data JDBC stores for agents and snapshots, Testcontainers tests,
  `postgres` service in the demo compose file. The agent coverage gate stays at the temporary
  ratchet (50 / 28 / 50 %) until M1.
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

Vertical slices towards the two screens, one PR each, in this order:

1. ARCHI-27: protobuf v1 redesign (scope, window, typed nodes, edge kinds and metrics), gRPC
   stream service, ingestion with validation into the stores, agent registry with liveness;
   minimal agent adaptation to the new contract (M2 PR 2).
2. ARCHI-28: current graph per scope at time T, scope list with counts, retention, metrics
   (M2 PR 3).
3. ARCHI-29: OpenAPI document and generator, scopes and agents endpoints, problem details
   (M3 PR 1); ARCHI-30: graph and services endpoints (M3 PR 2).
4. ARCHI-31: `architrace-ui` scaffold served from the control plane jar (M4 PR 1);
   ARCHI-32: Projects list page; ARCHI-33: Service map page; ARCHI-34: lenses, URL state, polish.
5. M1 (agent pipeline) follows so the demo stack feeds real data; until then a fixture loader
   seeds snapshots for development.

Before starting a slice: merge the open PR first (one PR at a time), rebase, update this page.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper).
