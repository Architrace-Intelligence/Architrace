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
  (ARCHI-52, #65). The `main` pipeline is green end to end; both images are in GHCR.
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
- **M6 is in progress.** PR 1 (ARCHI-53, #66) delivers the `rules` package of the control plane:
  `Finding` (with its `Severity`), the sealed `ArchitectureRule`, `RulesProperties`
  (`architrace.rules.*`, declared but not enabled yet), `RuleEngine`, and the rules
  `CyclicDependency` (strongly connected components of the sync edges, one shortest cycle as
  evidence), `SharedDatabase` (threshold `shared-database.min-services`, default 2) and
  `UnknownExternal` (`unknown-external.allowlist` of host names). Nothing runs the engine yet:
  the Spring wiring arrives with the evaluation trigger. PR 2 (ARCHI-54, #67) adds the blast
  radius: `ImpactAnalysis.of(graph, nodeId)` returns an `Optional<Impact>` (`impaired` by a
  reverse breadth-first search over sync edges, `delayed` one asynchronous passage further,
  paths from the node to the subject, lexicographically smallest among equally short paths)
  and the rule `WideBlastRadius` (`wide-blast-radius.min-share-percent` 50,
  `wide-blast-radius.min-services` 3; the share must exceed the percentage). PR 3 (ARCHI-55, #68)
  completes the rule set: Tarjan moves into the package-private
  `StronglyConnectedComponents`, shared by `CyclicDependency` and `LongSyncChain`;
  `CrossDomainCoupling` (`cross-domain-coupling.max-domains` 3, domain parsed from
  `service:{domain}/{name}`, services without a domain share the empty one), `FanInHub`
  (`fan-in-hub.max-callers` 8, direct service callers), `LongSyncChain`
  (`long-sync-chain.max-hops` 5: longest path over the condensation of the strongly connected
  components from every entry node, a cycle collapsed to its smallest member, databases and
  externals counted as the last hop, subjects and evidence are the path in order).
  PR 4 (ARCHI-56, #69) stores findings: `FindingStore` (`replace(scope, findings)`,
  `findings(scope)`) with `JdbcFindingStore` in `rules.persistence` over the `finding` table
  (Liquibase changelog `0002-finding.yaml`, scope columns, `subject_ids` and `evidence` as
  jsonb id lists, index on the scope); replacing is one transaction (delete the scope, insert
  the new rows); reads come back in `Finding.ORDER`. PR 5 (ARCHI-57, #70) runs the rules:
  `IngestionService` publishes a `SnapshotIngested` event after every stored snapshot,
  `RuleEvaluator` listens, claims the scope at most once per
  `architrace.rules.evaluation-interval` (30s, compare-and-set on a per-scope instant),
  evaluates the current graph at the control plane's `now` and replaces the findings of the
  scope; a failing evaluation is logged and never fails the ingestion. `RulesConfiguration`
  enables `RulesProperties` and the `RuleEngine` bean; the properties are on the
  configuration reference page. See the [M6](../features/m6-architecture-rules/) page;
  item 4 of its delivery plan is delivered in two pull requests (ARCHI-56 persistence,
  ARCHI-57 evaluation) because a pull request may change at most twelve files; the rule
  table there still needs the chain semantics and the plan the split (next docs touch).
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

M6 PR 1–4 (#66–#69) are merged. Merge M6 PR 5 (ARCHI-57, #70); the next pull request is
opened only after it is merged. Then continue, one pull request against `main` at a time,
each with the next free ticket number:

1. M6 PR 6: `findings` and `impact` endpoints (unknown node: 404 problem), OpenAPI schemas,
   the Query API reference page, finding counts on the scope summaries.
2. M6 PR 7 and following: Findings page, map badges, impact lens and rail card, user guide
   page.
3. M7 Packaging and demo (B5 health check on `/health`, B6 deployment guide for Spring Boot
   services, B7 demo TTL; the demo stack exercises the map, the drift and the findings with
   real data); the M6 candidates B2 and B3 along the way.
4. Agent follow-ups B1 (fold Kafka Streams internal topics) and B4 (sub-millisecond latency)
   as small pull requests; take over the Dependabot Gradle bumps of #47 in a maintainer PR.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
./gradlew printVersion
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper, Node downloaded by Gradle).
