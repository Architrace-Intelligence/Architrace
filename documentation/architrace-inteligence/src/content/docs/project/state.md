---
title: Current state
description: Where the work is right now and what the next step is. Read this first when resuming.
---

Hand-over page. It describes only the present: what is in flight, what is decided, what comes
next. History lives in [Progress](../progress/) and in git. Rewrite it before starting a new
task; delete anything that is no longer needed to resume.

Last updated: **2026-10-02**

## Where we are

- Stages 0–2 are done. Stage 3 (implementation) is open. M0 PR 1 (hygiene, ARCHI-24) is merged
  as #26; the remaining M0 PRs (build-logic, pipelines, release, ruleset) are deferred behind
  the feature slices the maintainer asked for on 2026-10-01: the Projects list and the Service
  map with their backend, step by step.
- The UI design (ARCHI-25) is merged as #30: eight interactive prototypes plus the canvas
  `MVP-frames` (initial release: Projects list and Service map) in the Claude Design project
  "Architrace UI", the [UI design](../features/ui-design/) page, sources under `design/`.
  The visual direction (dark-first "calm control room") was chosen without a brief and still
  needs the maintainer's confirmation or redirection; the tokens now live in
  `architrace-ui/src/styles/tokens.css`, so a redirection is a token edit.
- M2 (#31, #33, #35) and M3 (#36, #38) are complete: PostgreSQL schema and stores, ingestion,
  `TopologyQuery`, retention, Actuator, the OpenAPI 3.1 contract with generated server
  interfaces, every Query API resource, typed problem details, Swagger UI, the
  [Query API](../../reference/query-api/) reference. Every Java module carries a
  `gradle.lockfile`; a dependency change must rewrite them with `--write-locks`. The agent
  coverage ratchet is 52 / 31 / 50 % until M1 restores 85 %.
- M4 PR 1 (ARCHI-32, in review): module `architrace-ui` (Vite, React 19, TypeScript 5.9,
  TanStack Query, `openapi-fetch` with types from `openapi-typescript`), built by Gradle through
  the node plugin with a downloaded Node (`nodeVersion` in `gradle.properties`, installed under
  `.gradle/nodejs/`), bundle handed to the control plane as the consumable configuration
  `bundle` and served at `/` with `SpaFallbackResourceResolver` (HTML-accepting requests for
  extension-less paths outside `/api`, `/actuator`, `/swagger-ui`, `/webjars` get `index.html`).
  `npm run check` is the UI gate and runs as `:ui:test`; Spotless puts SPDX headers on the UI
  sources; Sonar and Dependabot cover the module. The first screen is a walking skeleton
  (scope count). Details on the [M4](../features/m4-service-map/) page.
- Collection processing uses the Stream API across both modules (maintainer, 2026-10-01;
  rule in `AGENTS.md` §4).
- Automation token for the GitHub API is issued and verified; git pushes use SSH.

## Decisions

Agreed and recorded (Requirements §7–8, ADR 0001–0010): PostgreSQL + Liquibase, CodeRabbit,
maintainer-authored PRs with a zero-approval ruleset, React + TypeScript UI, sequential
`ARCHI-<n>`, copyright holder. MVP scope M0–M7; design agreed on 2026-10-01.

The initial UI release is narrowed to two screens (maintainer, 2026-10-01): a Projects list
filtered by project, environment and cluster, and the Service map of the chosen scope with
its services and data streams.

Query API conventions since ARCHI-31: domain errors carry `urn:architrace:problem:<slug>`
types, framework errors none; the services list embeds the dependencies of every service
instead of a per-id resource; the snapshot history filters on window end, inclusive.

UI conventions since ARCHI-32: feature folders under `src/` (`app`, `api`, later `projects`,
`map`); server state only through `queryOptions` factories in `src/api/queries.ts`; the
generated `src/api/schema.d.ts` is never committed; tests stub `fetch` with the helpers in
`src/test/http.ts`; TypeScript stays on 5.x until `openapi-typescript` and `typescript-eslint`
support 6 and 7.

Working assumption since ARCHI-26: a **scope** is project × environment × cluster, reported by
the agent at registration and stored on every snapshot. Pending: where the project value comes
from on the agent side (resource attribute or agent setting), confirmation of the UI direction,
and the drift refinement proposed on the UI design page (compare deployments in timeline mode
only).

## Next step

M4 PR 1 (ARCHI-32) is open for review. Merge it first (one PR at a time), then rebase and
update this page.

Vertical slices towards the two screens, one PR each, in this order:

1. ARCHI-33: Projects list page on `GET /scopes` (frames 1–2 of `MVP-frames`: rows grouped by
   project, filter chips for environment and cluster, group-by control, URL state); the shell
   (navigation rail, top bar) comes with it.
2. ARCHI-34: Service map page on `…/graph` and `…/services` (React Flow + ELK, library decision
   with a 300-node fixture); ARCHI-35: lenses, panels, time selector, polish.
3. M1 (agent pipeline) follows so the demo stack feeds real data; until then a fixture loader
   seeds snapshots for development.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper, Node downloaded by Gradle).
