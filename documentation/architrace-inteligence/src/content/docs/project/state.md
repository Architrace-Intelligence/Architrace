---
title: Current state
description: Where the work is right now and what the next step is. Read this first when resuming.
---

Hand-over page. It describes only the present: what is in flight, what is decided, what comes
next. History lives in [Progress](../progress/) and in git. Rewrite it before starting a new
task; delete anything that is no longer needed to resume.

Last updated: **2026-10-06**

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
- **M0 is complete**, including the maintainer checklist on the
  [GitHub setup](../github-access/#6-setup-checklist-for-the-maintainer) page: the `main`
  ruleset is active, squash is the only merge method, CodeRabbit reviews every non-draft pull
  request,
  both GHCR packages are public and `v0.1.0` is released (the seed tag is `v0.0.0`).
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
- README badges (ARCHI-71, #93): the `main` pipeline runs the `writeBadges` task of the new
  `architrace.badges` convention plugin and publishes `badges/tests.json` (passed, failed,
  skipped over every JUnit report) and `badges/version.json` with the documentation site;
  the README shows the pipeline, CodeQL, tests, SonarCloud quality gate and coverage, Snyk, the
  release and the version on `main`, the images, and the stack versions read live from the
  repository files. The release badge shows `v0.1.0` since 2026-10-06.
- B2 from the real-data round (ARCHI-72, #94): the rule `DataStreamWithoutProducer` (id
  `data-stream-without-producer`, low, no threshold) reports a topic with consumers but no
  producer in the observed traces as the outbox insight; subject is the topic, evidence the
  consumers. Eight rules now, on the M6 page, the guide and the UI catalogue.
- B3 from the real-data round is closed in three pull requests (ARCHI-73, #95; ARCHI-74, #96;
  ARCHI-75): `architrace.topology.platform-hosts` names the platform services,
  `PlatformHosts.classify` labels their external nodes `category=platform` in
  `TopologyQuery.currentGraph`, `UnknownExternal` skips them, and the map keeps them off by
  default and groups them into one node `platform` behind the **Platform** chip
  (`platformView` in `map/model.ts`, `platform=on` in the URL; the drift map still shows them
  one by one). Design notes on the M4 and M6 pages.
- **M7 is done** (ARCHI-64 to ARCHI-67, #81 to #83 and #90): the demo stack `demo/` on the
  published images (two environments, one collector routing by environment to one agent each,
  Redpanda, PostgreSQL, an external host), the Docker demo and deployment guides, B5 to B7.
  Gotchas (the control plane's `architrace.ingestion.snapshot-interval` overrides the agent's
  interval; the Python requests instrumentation needs `OTEL_SEMCONV_STABILITY_OPT_IN=http`;
  `psycopg2-binary` needs `skip_dep_check`) are on the [M7](../features/m7-packaging-demo/)
  page. The MVP (M0 to M7) is complete.
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
`default`; the seed tag `v0.0.0` was created by hand, every release tag (`v0.1.0` first) is
computed and pushed by the main pipeline.

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

The MVP queue M0–M7, the real-data follow-ups B2 and B3 (#94 to #97) and the first release
`v0.1.0` are done. Merge ARCHI-76 (this documentation update). Then continue, one pull request
against `main` at a time, each with the next free ticket number:

1. Agent follow-ups B1 (fold Kafka Streams internal topics, ARCHI-77 next) and B4
   (sub-millisecond latency) as small pull requests; the `build-logic` JVM 24 pin (Kotlin 2.4 targets 25); Dependabot
   Gradle bumps need a maintainer pull request with regenerated lockfiles, as #74.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
./gradlew printVersion
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper, Node downloaded by Gradle).
