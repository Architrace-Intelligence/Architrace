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
- **M1 is done** (PR 1–5 merged as #50–#54): span model and attribute mapping, graph with
  ADR 0007 ids, paired edges and a pending index, snapshot publisher with a reconnecting
  session, Prometheus metrics and health, formatter on for the whole module, acceptance run
  of 6 000 000 spans in 600 s with 0 rejections and 20 MB heap. Details on the
  [M1](../features/m1-agent-pipeline/) page.
- **M4 PR 2 is merged (#55)**: the Projects list, the shell and `react-router` 8 in declarative
  mode; the merge confirmed the routing choice.
- **M4 PR 3 (ARCHI-44, #56) in review**: the Service map of a scope with React Flow 12 and the
  ELK layered layout, decided on the 300-node fixture (layout 0.5 s, initial bundle 156 kB
  gzipped, ELK as a lazy 436 kB chunk): node cards by type, edges by kind with width by calls
  and colour by error rate, node-type chips with counts carried in the URL, legend, minimap,
  fit to view.
- **M4 PR 4 (ARCHI-45) stacked on #56** (the maintainer allowed a stack to finish M4): lenses,
  the context rail with the scope, node and dependency panels, find-in-map, time selector
  (`graph?at=`), selection and time in the URL, keyboard selection, copy link. **Merge #56
  first**; the PR 4 branch is based on `ARCHI-44-service-map` and retargets to `main` once
  #56 is merged and its branch deleted. Details on the [M4](../features/m4-service-map/) page.
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

Merge #56 (M4 PR 3), then the PR 4 of ARCHI-45; M4 is complete with it. Then continue, one PR
each with the next free ticket number:

1. M5 Drift: `TopologyDiff` on the control plane (environment and timeline modes), the Query
   API endpoint, the Drift screen (grouped list, map overlays) as designed.
2. M6 Architecture rules, M7 Packaging and demo (the agent Dockerfile health check moves to
   `/health`; the demo stack of M7 exercises the map with real data).
3. Take over the Dependabot Gradle bumps of #47 in a maintainer PR on the way.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
./gradlew printVersion
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper, Node downloaded by Gradle).
