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
- Merged: M0 PR 1 (hygiene, #26) and PR 2 (build conventions and versioning, ARCHI-33, #42), the
  UI design (#30), M2 (#31, #33, #35), M3 (#36, #38), M4 PR 1 (#39). The UI direction (dark-first
  "calm control room", tokens in `architrace-ui/src/styles/tokens.css`) still awaits the
  maintainer's confirmation.
- **M0 PR 3 (ARCHI-34, in review)**: `pr.yml` with the required checks `build` (`./gradlew build`,
  reports and jars as artifacts), `quality` (SonarCloud over the restored reports, quality gate on
  new code) and `security` (Snyk high+, OWASP Dependency-Check CVSS ≥ 7 through the root Gradle
  plugin with `--no-parallel`, gitleaks binary over the pull request commits) plus `docs`;
  `pr-title.yml` checks the title; `codeql.yml` analyses `java-kotlin` (manual build),
  `javascript-typescript` and `actions`; the composite action `.github/actions/setup-build` reads
  the toolchain versions from `gradle.properties`; `agent.yml` removed; `docs-deploy.yml`
  restricted to `main`. Details on the [M0](../features/m0-engineering-platform/) page.
- M0 PR 3 is merged (#44). Scanner credentials are in place since 2026-10-02: `SNYK_TOKEN` is
  the Snyk PAT `architrace-ci` (Snyk PATs live 90 days at most, this one expires 2026-12-31;
  service accounts are a paid feature), `SNYK_ORG` is the real slug `architrace`, `NVD_API_KEY`
  is an activated NVD key; all of them exist as repository and as Dependabot secrets.
- **M0 PR 4 (ARCHI-36, in review)**: `main.yml` replaces `ci-cd.yml` and `docs-deploy.yml`:
  the three gates of `pr.yml`, then `release` (tag, notes, jars under the tag, GitHub release;
  skipped with a notice while no `v*` tag exists), `images` (module Dockerfiles over the prebuilt
  jars, Trivy SARIF plus a gate on fixable critical findings, GHCR tags `sha-<short>` always and
  `X.Y.Z` + `latest` on release), `docs` deploy to Pages and Gradle dependency-graph submission.
  Dockerfiles `architrace-agent/Dockerfile` and `architrace-control-plane/Dockerfile` verified
  locally. Details on the [M0](../features/m0-engineering-platform/) page.
- **Dependency update (ARCHI-35, merged as #46)**: the first Snyk run with valid credentials found
  22 critical and 110 high findings across the Java modules and the docs site. The PR moves
  Spring Boot 4.0.3 → 4.1.1 and the control plane to the Boot gRPC server starter (Spring gRPC 1.1
  moved its starters and test support into Boot, its BOM now manages only `spring-grpc-core`),
  overrides the Boot-managed Tomcat (11.0.26), Netty (4.2.18), Jackson 2 (2.22.3), grpc-java
  (1.84.0) and protobuf-java (4.36.2, gencode and runtime must match) versions from the catalog,
  lifts the rest of the catalog to the latest releases (protobuf plugin 0.10.0 needs
  `maybeCreate` for the per-task grpc plugin options), moves the docs site to Astro 7 /
  Starlight 0.42 (sidebar `autogenerate` groups became `items`), and tells Dependabot to ignore
  TypeScript majors for the UI. The root `gradle.lockfile` is gone: the root project has no
  lockable configurations and the old file only carried stale GraalVM entries.
- Still open for the maintainer, after PR 4 is merged: seed the tag (`git tag -a v0.1.0 -m v0.1.0
  <merge commit> && git push origin v0.1.0`; from then on every `feat`/`fix` merge releases),
  make the two GHCR packages public after the first `main` run, close Dependabot #45 (edits the
  removed `ci-cd.yml`). Dependabot Gradle PRs (#47) fail `build` because Dependabot does not
  regenerate lockfiles; take their bumps into maintainer PRs.
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

Working assumption since ARCHI-26: a **scope** is project × environment × cluster, reported by
the agent at registration and stored on every snapshot. Pending: where the project value comes
from on the agent side (resource attribute or agent setting), confirmation of the UI direction,
and the drift refinement proposed on the UI design page (compare deployments in timeline mode
only).

## Next step

M0 PR 4 (ARCHI-36) is open for review. Merge it first (one PR at a time), watch the first
`main` run (images, notice about the missing seed tag), seed `v0.1.0`, then update this page.

Then, in order (one PR each, next free ticket number):

1. M0 PR 5: `main` ruleset (ADR 0003) with the required checks `build`, `quality`, `security`,
   `docs`, `title` and the CodeQL analyses; repository settings (squash only, delete branch on
   merge, Dependabot alerts and security updates, secret scanning with push protection);
   `.coderabbit.yaml`; site pages.
2. M1 agent pipeline (restores 85 % coverage and the formatter in the agent), then M4 PR 2
   (Projects list), M4 PR 3–4, M5, M6, M7.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
./gradlew printVersion
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper, Node downloaded by Gradle).
