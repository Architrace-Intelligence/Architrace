---
title: Current state
description: Where the work is right now and what the next step is. Read this first when resuming.
---

Hand-over page. It describes only the present: what is in flight, what is decided, what comes
next. History lives in [Progress](../progress/) and in git. Rewrite it before starting a new
task; delete anything that is no longer needed to resume.

Last updated: **2026-10-04**

## Where we are

- Stages 0–2 are done. Stage 3 (implementation) is open. Order of work since 2026-10-02: finish
  **M0** first, then M1, M4 PR 2 onwards, M5, M6, M7; the vertical UI slices are paused after
  M4 PR 1.
- Merged: M0 PR 1–5 (#26, #42, #44, #48, #49), the dependency update (ARCHI-35, #46), the UI
  design (#30), M2 (#31, #33, #35), M3 (#36, #38), M4 PR 1 (#39). The second `main` run
  (2026-10-04) was green end to end: both images are in GHCR as `sha-f0c43ab`.
- **M0 is complete in the repository.** What remains is the maintainer checklist on the
  [GitHub setup](../github-access/#6-setup-checklist-for-the-maintainer) page: apply the
  ruleset (`.github/rulesets/main.json`) and the repository settings (§5 there), install the
  CodeRabbit app, make the two GHCR packages public, seed `v0.1.0` (`git tag -a v0.1.0 -m v0.1.0
  f0c43ab && git push origin v0.1.0`). Dependabot #45 is closed. Until the ruleset is active
  the merge gate is discipline, not platform.
- **M1 PR 1 merged (#50)**; **M1 PR 2 (ARCHI-39, #51), PR 3 (ARCHI-40) and PR 4 (ARCHI-41) in
  review**, each branched from the previous one and opened after it merges. PR 4: `metrics`
  package (`AgentMetrics` binding, `MetricsServer` with `/metrics` and `/health`,
  `DropReporter`), counters on the receiver, queues, builder and publisher, `loadTest` Gradle
  task (tag `load`, `-Pload.seconds`). PR 2: `graph` package (sealed `GraphNode` with
  ADR 0007 ids, `EdgeBuilder`, `PendingSpanIndex`, `LatencyHistogram`, `GraphWindow`,
  `GraphBuilder`) and `pipeline` package (`SpanQueue`, `GraphWorker`). PR 3: `publish` package
  (`SnapshotProtoMapper`, `SnapshotQueue`, `PublisherStats`), `ControlPlaneSession` and
  `ControlPlaneSupervisor`, `TransportClient` reduced to open/close, `graph_batch` removed from
  the proto (field 2 reserved), the agent's server-side classes and the old session wiring
  removed, end-to-end runtime test with an in-process stub control plane. Ratchet: line 0.85 /
  branch 0.84 / method 0.85. Details on the [M1](../features/m1-agent-pipeline/) page.
- The maintainer asked on 2026-10-04 to leave the ruleset aside and implement M1 and M4 first;
  testing follows.
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
`openapi-typescript` and `typescript-eslint` support 6 and 7.

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

Merge #51 (M1 PR 2), then the PRs of ARCHI-40 (M1 PR 3) and ARCHI-41 (M1 PR 4), in that
order. Then continue, one PR each with the next free ticket number:

1. M1 PR 5: formatter on for the agent (the remaining 2-space files: `MainApp`, `cli`,
   `core.BuildVersion`, `otlp` receiver and server, `grpc.GrpcAddressParser` and their tests),
   the ten-minute load run recorded on the M1 page, M1 marked done; the agent Dockerfile
   health check can move to `/health` (M7 owns the image).
2. M4 PR 2: Projects list (scopes with agents, services, data streams, last snapshot) on the
   agreed UI design; then M4 PR 3–4 (Service map), M5, M6, M7.
3. Take over the Dependabot Gradle bumps of #47 in a maintainer PR on the way.

Then M4 PR 2 (Projects list), M4 PR 3–4, M5, M6, M7.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
./gradlew printVersion
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper, Node downloaded by Gradle).
