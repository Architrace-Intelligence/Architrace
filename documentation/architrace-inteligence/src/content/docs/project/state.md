---
title: Current state
description: Where the work is right now and what the next step is. Read this first when resuming.
---

Hand-over page. It describes only the present: what is in flight, what is decided, what comes
next. History lives in [Progress](../progress/) and in git. Rewrite it before starting a new
task; delete anything that is no longer needed to resume.

Last updated: **2026-10-02**

## Where we are

- Stages 0–2 are done. Stage 3 (implementation) is open. On 2026-10-02 the maintainer asked to
  finish **M0 first and then continue in order** (M1, then M4 PR 2 onwards, M5, M6, M7); the
  vertical UI slices are paused after M4 PR 1.
- Merged: M0 PR 1 (hygiene, #26), the UI design (#30), M2 (#31, #33, #35), M3 (#36, #38), M4 PR 1
  (#39: `architrace-ui` scaffold, SPA serving, typed client, UI gate). The UI direction (dark-first
  "calm control room", tokens in `architrace-ui/src/styles/tokens.css`) still awaits the
  maintainer's confirmation.
- **M0 PR 2 (ARCHI-33, in review)**: included build `build-logic` with `architrace.java`,
  `architrace.spring-boot` and `architrace.versioning`; project paths equal directory names
  (`:architrace-agent`, …) and artifacts are named after them; version from git tags and
  Conventional Commits through axion-release (`printVersion`, `printReleaseVersion`,
  `releaseNotes`; no version in `gradle.properties`); the CLI and the agent registration read
  the version from the jar manifest (`core.BuildVersion`); palantir-java-format (4 spaces, 120
  columns) and Checkstyle 14 (`config/checkstyle/checkstyle.xml`, comments forbidden) run inside
  `check`; the gate command is `./gradlew spotlessApply check`. The agent keeps
  `java.format.enabled=false` and the coverage ratchet 52 / 31 / 50 % until M1. Details on the
  [M0](../features/m0-engineering-platform/) page.
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

Working assumption since ARCHI-26: a **scope** is project × environment × cluster, reported by
the agent at registration and stored on every snapshot. Pending: where the project value comes
from on the agent side (resource attribute or agent setting), confirmation of the UI direction,
and the drift refinement proposed on the UI design page (compare deployments in timeline mode
only).

## Next step

M0 PR 2 (ARCHI-33) is open for review. Merge it first (one PR at a time), then rebase and
update this page.

Then, in order (one PR each, next free ticket number):

1. M0 PR 3: `pr.yml` with `build → quality → security` jobs and the title check, `codeql.yml`,
   composite action for JDK + Gradle, Snyk / OWASP Dependency-Check / Gitleaks configuration,
   removal of `agent.yml` and `ci-cd.yml` once `main.yml` exists.
2. M0 PR 4: `main.yml` with release (tag + GitHub release from `releaseNotes` when
   `printReleaseVersion` differs from the latest tag) and images (Dockerfiles that copy prebuilt
   jars, Trivy, GHCR); the maintainer seeds `v0.1.0` beforehand.
3. M0 PR 5: `main` ruleset (ADR 0003), repository settings, `.coderabbit.yaml`, site pages.
4. M1 agent pipeline (restores 85 % coverage and the formatter in the agent), then M4 PR 2
   (Projects list), M4 PR 3–4, M5, M6, M7.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
./gradlew printVersion
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper, Node downloaded by Gradle).
