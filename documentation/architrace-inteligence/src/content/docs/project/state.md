---
title: Current state
description: Where the work is right now and what the next step is. Read this first when resuming.
---

Hand-over page. It describes only the present: what is in flight, what is decided, what comes
next. History lives in [Progress](../progress/) and in git. Rewrite it before starting a new
task; delete anything that is no longer needed to resume.

Last updated: **2026-10-05**

## Where we are

- Stages 0–2 are done. Stage 3 (implementation) is open; stage 4 (testing, hardening) has
  started with the first real-data round. Order of work since 2026-10-02: M0, M1, M4, then M5,
  M6, M7.
- Merged: M0 PR 1–5 (#26, #42, #44, #48, #49), the dependency update (ARCHI-35, #46), the UI
  design (#30), M1 (#50–#54), M2 (#31, #33, #35), M3 (#36, #38), M4 PR 1–5 (#39, #55, #56, #58,
  #59), the real-data backlog (ARCHI-47, #60), M5 PR 1–2 (#61, #62), the M6 blast-radius
  design (ARCHI-50, #63), the pull request size gate (ARCHI-51, #64) and the pipeline order
  (ARCHI-52, #65), M6 (#66–#70, #75–#80), the Snyk hotfix (#71) and Gradle 9.8.0 (#74). The
  `main` pipeline is green end to end; both images are in GHCR.
- **M0 is complete in the repository.** What remains is the maintainer checklist on the
  [GitHub setup](../github-access/#6-setup-checklist-for-the-maintainer) page: apply the
  ruleset (`.github/rulesets/main.json`) and the repository settings (§5 there), install the
  CodeRabbit app, make the two GHCR packages public, seed `v0.1.0`. Until the ruleset is active
  the merge gate is discipline, not platform.
- **M1, M4 and M5 are done**, see the [M1](../features/m1-agent-pipeline/),
  [M4](../features/m4-service-map/) and [M5](../features/m5-drift/) pages. Lesson recorded on
  2026-10-04: **no stacked pull requests**, one pull request against `main` at a time. Working
  assumption to confirm for M5: environment mode compares versions only, timeline mode versions
  and deployments.
- **M6 is done** (ARCHI-53 to ARCHI-63, #66–#70 and #75–#80): seven deterministic rules, the
  `finding` table, evaluation after every ingested snapshot, the `findings` and `impact`
  endpoints, the Findings screen, badges on the Projects list and the map, the impact lens and
  the user guide. Semantics and the eleven-step delivery plan are on the
  [M6](../features/m6-architecture-rules/) page; candidates B2 and B3 stay open there.
- Housekeeping merged on 2026-10-05: the Snyk policy of the documentation site (ARCHI-68, #71:
  `overrides` for `postcss-selector-parser`, a time-boxed ignore for `zod`) and Gradle 9.8.0
  with regenerated lockfiles (ARCHI-69, #74; the unused `guava` catalog entry is gone). Open
  follow-up: drop the JVM 24 pin in `build-logic` now that Kotlin 2.4 targets 25. The demo's
  Python dependencies move together (ARCHI-70, #91: OpenTelemetry SDK and exporter 1.45.0,
  instrumentations 0.66b0, kafka-python 3.0.11, psycopg2-binary 2.9.13) and Dependabot now
  groups the pip updates of `demo/services`, because the SDK, exporter and instrumentation
  pins only resolve as one set and no CI job builds the demo image; the stack was run end to
  end with locally built images after the bump.
- **M7 is in progress.** PR 1 (ARCHI-64, #81) adds `demo/`: `docker-compose.yml` with the
  control plane, one agent per environment (`agent-dev.yaml`, `agent-stage.yaml`), one
  OpenTelemetry Collector routing on `deployment.environment.name`, PostgreSQL, Redpanda, an
  external host and four Flask services from one parameterised `services/app.py` (`ROLE`), plus
  the rewritten [Docker demo](../../guides/docker-demo/) guide. Validated end to end with
  locally built images (DEV: cycle,
  shared database, three blast radii, unknown external; STAGE drift: version and missing
  back-call). PR 2 (ARCHI-65, #82) points the README, `AGENTS.md`, `SECURITY.md`, the
  getting-started, local-development and modules pages, the architecture page and the M7
  page at `demo/`. Gotchas recorded on the M7 page: the control plane's
  `architrace.ingestion.snapshot-interval` overrides the agent's interval; the Python
  requests instrumentation needs `OTEL_SEMCONV_STABILITY_OPT_IN=http` to emit
  `server.address`; `psycopg2-binary` needs `skip_dep_check`. PR 3 (ARCHI-66, #83) removes
  `otel-test-app/` and moves the Snyk excludes and the Dependabot pip entry to `demo/`. PR 4
  (ARCHI-67, #90) closes M7: the deployment guide (control plane, one agent per environment,
  collector snippet, the Spring Boot instrumentation section of B6, a checklist), B5 (the
  agent image's health check probes `/health` on `ARCHITRACE_METRICS_PORT`, `4319` is the one
  default OTLP port, the demo drops its workaround) and B7 (the delay explained next to
  `buffers.pending-ttl-seconds`). The GHCR packages are still private (maintainer
  checklist), so `docker compose up` on a clean machine waits for that; the MVP (M0 to M7) is
  complete once #90 is merged.
- Backlog B1–B7 from the first real-data round is in
  [Requirements §9](../requirements/#9-backlog-from-the-first-real-data-test-round) and on
  the M1, M6 and M7 pages.
- The agent session cannot write repository settings or rulesets (its tool permissions stop at
  administration writes); the maintainer applies them.
- Collection processing uses the Stream API across both modules (maintainer, 2026-10-01;
  rule in `AGENTS.md` §4).
- Automation token for the GitHub API is issued and verified; git pushes use SSH.

## Decisions

Agreed and recorded (Requirements §7–8, ADR 0001–0010): PostgreSQL + Liquibase, CodeRabbit,
maintainer-authored PRs with a zero-approval ruleset, React + TypeScript UI, sequential
`ARCHI-<n>`, copyright holder. MVP scope M0–M7; design agreed on 2026-10-01. Order of work
since 2026-10-02: M0 to completion, then M1, M4, M5, M6, M7.

The initial UI release is narrowed to two screens (maintainer, 2026-10-01): a Projects list
filtered by project, environment and cluster, and the Service map of the chosen scope with
its services and data streams.

Query API conventions since ARCHI-31: domain errors carry `urn:architrace:problem:<slug>`
types, framework errors none; the services list embeds the dependencies of every service
instead of a per-id resource; the snapshot history filters on window end, inclusive. Since
ARCHI-48: a diff is a resource of the scope that is its right side; the left side comes as
query parameters; everything "added" is only on the right, "removed" only on the left.

Rules conventions since ARCHI-53: a rule is a record that carries its own thresholds and is
built from `RulesProperties` by `RuleEngine.of`; rule ids are kebab-case
(`cyclic-dependency`, `shared-database`, `unknown-external`) and will be the `rule` filter of
the findings endpoint; `subjectNodeIds` and `evidence` hold node ids, names appear only in
`title` and `detail`; findings are ordered by severity, rule id, then subject ids; the
evidence of a cycle is the shortest cycle through its smallest node id; self-calls and paths
through topics are never cycles; the external allowlist matches the host name.

UI conventions since ARCHI-32: feature folders under `src/`; server state only through
`queryOptions` factories; the generated `src/api/schema.d.ts` is never committed; tests stub
`fetch` with the helpers in `src/test/http.ts`; TypeScript stays on 5.x until
`openapi-typescript` and `typescript-eslint` support 6 and 7. Since ARCHI-44: the map derives
everything from the one `…/graph` request; a layout is a TanStack query keyed by node and edge
ids; edge kind and health are class names styled through the tokens; React Flow's `--xy-*`
variables map onto the tokens in `base.css`; the jsdom stubs React Flow needs live in
`src/test/setup.ts`. Since ARCHI-49: the Drift screen is a route of the scope that is its
right side; defaults that the URL lacks are filled by a replace `Navigate`, never by effects;
the map draws a diff through an overlay prop, not through a second map.

Build conventions since ARCHI-33: new Java modules apply `architrace.java` (or
`architrace.spring-boot`); third-party plugins are applied by id, their versions live in the
catalog and on the `build-logic` classpath; switches stay exhaustive instead of carrying a
`default`; the first release tag `v0.1.0` is created manually by the maintainer, everything
after it is computed.

Pipeline conventions since ARCHI-34: every job name is a required-check name; actions are pinned
by commit SHA with the version in a trailing comment (Dependabot keeps both current); a scanner
whose action needs a licence runs as a pinned, checksum-verified binary instead; a step that needs
a secret skips with a notice when the secret is absent (Dependabot runs) rather than failing;
the Gradle gate in CI is the same command as locally. Since ARCHI-51 and ARCHI-52: the `size`
job fails a pull request with more than twelve changed files and runs first; CodeQL runs after
the build.

Merge gate conventions since ARCHI-37: the ruleset lives in `.github/rulesets/main.json` and
changes with the job names it requires; squash merge only, the pull request title is the commit
subject; branches need not be up to date with `main` (one pull request at a time); CodeRabbit
threads are resolved by a fix or an answer.

Agent conventions since ARCHI-38: absent record components are `Optional`, never `null`; the
Jackson-facing document record and the effective configuration record are separate types;
new agent files are written in palantir format even though the module-wide formatter switch
stays off until the legacy files are rewritten (M1 PR 5); the agent coverage ratchet only
moves up. Working assumption: `project` is an agent setting, not a telemetry attribute.

Working assumption since ARCHI-26: a **scope** is project × environment × cluster, reported by
the agent at registration and stored on every snapshot. Pending: confirmation of the UI
direction and the drift refinement proposed on the UI design page (compare deployments in
timeline mode only).

## Next step

The MVP queue M0–M7 is merged (#90 closed M7). Merge the demo dependency bump (ARCHI-70, #91),
which also retires the open Dependabot pull requests on `demo/services`. Then continue, one
pull request against `main` at a time, each with the next free ticket number:

1. The M6 candidates B2 (an outbox topic with consumers but no producer as an insight) and
   B3 (a platform category in the external allowlist); the maintainer checklist of M0
   (ruleset, CodeRabbit, public GHCR packages, `v0.1.0`).
2. Agent follow-ups B1 (fold Kafka Streams internal topics) and B4 (sub-millisecond latency)
   as small pull requests; the `build-logic` JVM 24 pin (Kotlin 2.4 targets 25); Dependabot
   Gradle bumps need a maintainer pull request with regenerated lockfiles, as #74.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
./gradlew printVersion
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper, Node downloaded by Gradle).
