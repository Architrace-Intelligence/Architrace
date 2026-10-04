---
title: Current state
description: Where the work is right now and what the next step is. Read this first when resuming.
---

Hand-over page. It describes only the present: what is in flight, what is decided, what comes
next. History lives in [Progress](../progress/) and in git. Rewrite it before starting a new
task; delete anything that is no longer needed to resume.

Last updated: **2026-10-04**

## Where we are

- Stages 0–2 are done. Stage 3 (implementation) is open; stage 4 (testing, hardening) has
  started with the first real-data round. Order of work since 2026-10-02: M0, M1, M4, then M5,
  M6, M7.
- Merged: M0 PR 1–5 (#26, #42, #44, #48, #49), the dependency update (ARCHI-35, #46), the UI
  design (#30), M1 (#50–#54), M2 (#31, #33, #35), M3 (#36, #38), M4 PR 1–5 (#39, #55, #56, #58,
  #59). The `main` pipeline is green end to end; both images are in GHCR.
- **M0 is complete in the repository.** What remains is the maintainer checklist on the
  [GitHub setup](../github-access/#6-setup-checklist-for-the-maintainer) page: apply the
  ruleset (`.github/rulesets/main.json`) and the repository settings (§5 there), install the
  CodeRabbit app, make the two GHCR packages public, seed `v0.1.0`. Until the ruleset is active
  the merge gate is discipline, not platform.
- **M1 and M4 are done**, see the [M1](../features/m1-agent-pipeline/) and
  [M4](../features/m4-service-map/) pages. Lesson recorded on 2026-10-04: **no stacked pull
  requests**, one pull request against `main` at a time.
- **First real-data round (2026-10-04)**: `main` at b1579fe ran unchanged against a private
  eight-service stack (HTTP, JDBC and R2DBC, Spring Kafka, reactor-kafka, Kafka Streams,
  Debezium outbox, Redis) driven by its end-to-end suite; 331 063 spans, 0 rejected, 52
  snapshots acknowledged; Prometheus output of the instrumented services unchanged. The backlog
  B1–B7 is in [Requirements §9](../requirements/#9-backlog-from-the-first-real-data-test-round)
  and on the M1, M6 and M7 pages (ARCHI-47). The stack itself is private and is not described
  in this repository.
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
instead of a per-id resource; the snapshot history filters on window end, inclusive.

UI conventions since ARCHI-32: feature folders under `src/`; server state only through
`queryOptions` factories; the generated `src/api/schema.d.ts` is never committed; tests stub
`fetch` with the helpers in `src/test/http.ts`; TypeScript stays on 5.x until
`openapi-typescript` and `typescript-eslint` support 6 and 7. Since ARCHI-44: the map derives
everything from the one `…/graph` request; a layout is a TanStack query keyed by node and edge
ids; edge kind and health are class names styled through the tokens; React Flow's `--xy-*`
variables map onto the tokens in `base.css`; the jsdom stubs React Flow needs live in
`src/test/setup.ts`.

Build conventions since ARCHI-33: new Java modules apply `architrace.java` (or
`architrace.spring-boot`); third-party plugins are applied by id, their versions live in the
catalog and on the `build-logic` classpath; switches stay exhaustive instead of carrying a
`default`; the first release tag `v0.1.0` is created manually by the maintainer, everything
after it is computed.

Pipeline conventions since ARCHI-34: every job name is a required-check name; actions are pinned
by commit SHA with the version in a trailing comment (Dependabot keeps both current); a scanner
whose action needs a licence runs as a pinned, checksum-verified binary instead; a step that needs
a secret skips with a notice when the secret is absent (Dependabot runs) rather than failing;
the Gradle gate in CI is the same command as locally.

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

Merge the docs pull request of ARCHI-47 (backlog B1–B7). Then continue, one PR each with the
next free ticket number:

1. M5 Drift: `TopologyDiff` on the control plane (environment and timeline modes), the Query
   API endpoint, the Drift screen (grouped list, map overlays) as designed.
2. M6 Architecture rules (with the candidates B2 and B3), M7 Packaging and demo (B5 health
   check on `/health`, B6 deployment guide for Spring Boot services, B7 demo TTL; the demo stack
   exercises the map with real data).
3. Agent follow-ups B1 (fold Kafka Streams internal topics) and B4 (sub-millisecond latency)
   as small PRs when M5 is out of the way.
4. Take over the Dependabot Gradle bumps of #47 in a maintainer PR on the way.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
./gradlew printVersion
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper, Node downloaded by Gradle).
