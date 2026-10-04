---
title: Progress
description: Where the project is, stage by stage and feature by feature.
---

Single place to see where the project is. Updated whenever a stage or a feature changes state.

Legend: `todo` · `in progress` · `review` · `done` · `blocked`

## Stages

| # | Stage                                   | State       | Notes                                                  |
|---|-----------------------------------------|-------------|--------------------------------------------------------|
| 0 | Project analysis and requirements       | done        | [Requirements](../requirements/) merged in PR #20      |
| 1 | MVP scope agreement                     | done        | M0–M7 agreed, [Requirements §8](../requirements/#8-agreed-mvp) |
| 2 | Design (architecture, ADRs, features)   | done        | Agreed 2026-10-01: architecture, M0–M7, ADR 0001–0010  |
| 3 | Implementation (feature by feature)     | in progress | M2, M3 and M4 PR 1 done; M0 PR 1–4 merged; M0 PR 5 (merge gate, CodeRabbit, ARCHI-37) in review |
| 4 | Testing, hardening, release 1.0         | todo        |                                                        |

## Process foundation

| Item                                              | State | PR / note                                   |
|---------------------------------------------------|-------|---------------------------------------------|
| AGENTS.md                                         | done  | PR #20                                     |
| Requirements document                             | done  | PR #20                                     |
| Progress document                                 | done  | PR #20                                     |
| GitHub access and setup checklist                 | done  | PR #20, [GitHub setup](../github-access/)   |
| Pull request template                             | done  | PR #20                                     |
| Documentation moved to the site, current-state page | done  | PR #21                                   |
| Commit convention and automated versioning        | done  | ADR 0006; `architrace.versioning` (ARCHI-33, #42) |
| CI: single PR pipeline with quality gates         | done  | `pr.yml`, `pr-title.yml`, composite action (ARCHI-34, #44) |
| CI: AI code review                                | review | `.coderabbit.yaml` (ARCHI-37); the maintainer installs the app |
| CI: security scanning (Snyk, OWASP, CodeQL, …)    | done  | Snyk, Dependency-Check, gitleaks, CodeQL (ARCHI-34); credentials and clean baseline (ARCHI-35) |
| CD: main pipeline, release, images                | done  | `main.yml`, Dockerfiles, Trivy, GHCR, dependency graph (ARCHI-36, #48); first run green on 2026-10-04 |
| Branch ruleset on `main`                          | review | `.github/rulesets/main.json` (ARCHI-37); the maintainer applies it |
| ADR folder and first ADRs                         | done  | ADR 0001–0010 accepted                      |
| Architecture page                                 | done  | current and target views                    |
| Repository hygiene (templates, stale files)       | done  | M0 PR 1 (ARCHI-24), PR #26                  |

## Features

Filled in once the MVP scope is agreed. One row per feature, linked to its page under `project/features/`.

| Feature | State | Design | PR | Notes |
|---------|-------|--------|----|-------|
| [M0 Engineering platform](../features/m0-engineering-platform/) | in progress | agreed | ARCHI-24, ARCHI-33, ARCHI-34, ARCHI-36, ARCHI-37 | PR 1 (hygiene, #26), PR 2 (`build-logic`, versioning, #42), PR 3 (PR pipeline, scanners, #44), PR 4 (main pipeline, release, images, #48) merged; PR 5 (ruleset, settings, CodeRabbit, site pages) in review |
| [M1 Agent pipeline completion](../features/m1-agent-pipeline/) | todo | agreed | | |
| [M2 Control plane ingestion and storage](../features/m2-control-plane-storage/) | done | agreed | ARCHI-26, ARCHI-28, ARCHI-29 | schema and stores, ingestion, current graph and retention |
| [M3 Query API](../features/m3-query-api/) | done | agreed | ARCHI-30, ARCHI-31 | document and generator; scopes, agents, graph, services, snapshot history; typed problem details; reference page |
| [M4 Service map UI](../features/m4-service-map/) | in progress | agreed · [UI design](../features/ui-design/) in review | ARCHI-25, ARCHI-32 | PR 1 of 4 merged (#39): `architrace-ui` scaffold, Gradle and SPA integration, typed client, UI gate |
| [M5 Drift](../features/m5-drift/) | todo | agreed · UI design in review | | |
| [M6 Architecture rules](../features/m6-architecture-rules/) | todo | agreed · UI design in review | | |
| [M7 Packaging and demo](../features/m7-packaging-demo/) | in progress | agreed | ARCHI-36 | images delivered with M0 PR 4; demo stack and guides pending |

## History

| Date       | Event                                                        |
|------------|--------------------------------------------------------------|
| 2026-10-01 | Project analysed, requirements and process documents drafted |
| 2026-10-01 | Automation token issued and verified; documentation moved to the site |
| 2026-10-01 | Decisions recorded: storage, AI reviewer, review identity, ticket numbering, copyright |
| 2026-10-01 | UI stack decided: React + TypeScript (ADR 0004) |
| 2026-10-01 | MVP scope M0–M7 agreed; design stage opened |
| 2026-10-01 | Design PR: architecture page, feature pages M0–M7, ADR 0005–0010 proposed |
| 2026-10-01 | Design agreed by the maintainer; ADR 0005–0010 accepted; implementation starts with M0 |
| 2026-10-01 | M0 PR 1: repository hygiene, build green, community files, labels created |
| 2026-10-01 | M0 PR 1 merged (#26): main builds green again |
| 2026-10-01 | UI design: interactive prototypes of the MVP screens, foundations and agent-first principles (ARCHI-25) |
| 2026-10-01 | Initial UI release narrowed to Projects list and Service map; five static frames added to the design (ARCHI-25) |
| 2026-10-01 | M2 PR 1: PostgreSQL schema, Spring Data JDBC stores, Testcontainers (ARCHI-26) |
| 2026-10-01 | Gradle dependency locking for every module, fixes the SonarCloud finding on build files (ARCHI-27) |
| 2026-10-01 | M2 PR 2: contract extended with scope and snapshots, ingestion, agent registry and liveness (ARCHI-28) |
| 2026-10-01 | M2 PR 3: current graph per scope at a point in time, scope summaries for the Projects list, retention job, Actuator metrics and health (ARCHI-29) |
| 2026-10-01 | M3 PR 1: OpenAPI 3.1 contract, generated server interfaces, `GET /scopes` and `GET /agents`, Swagger UI, problem details (ARCHI-30) |
| 2026-10-02 | M3 PR 2: graph, services and snapshot history endpoints, single snapshot, typed problem details, Query API reference with examples (ARCHI-31) |
| 2026-10-02 | M4 PR 1: `architrace-ui` module (Vite, React, TypeScript) built by Gradle with a downloaded Node, bundle served by the control plane with a single-page fallback, client typed from the OpenAPI document, UI quality gate in the pipelines (ARCHI-32) |
| 2026-10-02 | Maintainer: finish M0 first, then continue in order (M1, M4, M5, M6, M7) |
| 2026-10-02 | M0 PR 2: `build-logic` convention plugins, version from git tags and Conventional Commits, CLI version from the manifest, palantir-java-format and Checkstyle, project paths equal directory names (ARCHI-33) |
| 2026-10-02 | M0 PR 2 merged (#42) |
| 2026-10-02 | M0 PR 3: `pr.yml` with `build → quality → security` and `docs`, title check, CodeQL for Java, TypeScript and workflows, Snyk, OWASP Dependency-Check, gitleaks, composite setup action (ARCHI-34) |
| 2026-10-02 | M0 PR 3 merged (#44); Snyk token rotated, NVD API key activated, scanner secrets mirrored to Dependabot |
| 2026-10-04 | Dependency update: Spring Boot 4.1.1 with the Boot gRPC starter, managed-version overrides, catalog on the latest releases, docs site on Astro 7; closes the first Snyk and Dependency-Check findings (ARCHI-35) |
| 2026-10-04 | Dependency update merged (#46): Snyk and Dependency-Check report zero findings |
| 2026-10-04 | M0 PR 4: `main.yml` with release, images in GHCR, Trivy, Pages deploy and dependency graph; module Dockerfiles (ARCHI-36) |
| 2026-10-04 | M0 PR 4 merged (#48): first `main` run passes the gates and reports the missing seed tag `v0.1.0`; the `images` jobs fail at the Trivy scan (image reference mismatch, fixed in ARCHI-37) |
| 2026-10-04 | M0 PR 5: `main` ruleset as code, squash-only settings, `.coderabbit.yaml`, contributing and getting-started pages (ARCHI-37) |
