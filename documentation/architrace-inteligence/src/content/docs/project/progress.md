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
| 3 | Implementation (feature by feature)     | done        | M1 to M7 done; M0 checklist done on 2026-10-06 (ruleset, settings, public packages, CodeRabbit, `v0.1.0`) |
| 4 | Testing, hardening, release 1.0         | in progress | First real-data round on 2026-10-04: no Architrace change needed, backlog B1–B7 in [Requirements §9](../requirements/#9-backlog-from-the-first-real-data-test-round) |

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
| CI: AI code review                                | done  | `.coderabbit.yaml` (ARCHI-37, #49); the maintainer installs the app |
| CI: security scanning (Snyk, OWASP, CodeQL, …)    | done  | Snyk, Dependency-Check, gitleaks, CodeQL (ARCHI-34); credentials and clean baseline (ARCHI-35) |
| CD: main pipeline, release, images                | done  | `main.yml`, Dockerfiles, Trivy, GHCR, dependency graph (ARCHI-36, #48); first run green on 2026-10-04 |
| Branch ruleset on `main`                          | done  | `.github/rulesets/main.json` (ARCHI-37, #49); the maintainer applies it |
| ADR folder and first ADRs                         | done  | ADR 0001–0010 accepted                      |
| Architecture page                                 | done  | current and target views                    |
| Repository hygiene (templates, stale files)       | done  | M0 PR 1 (ARCHI-24), PR #26                  |

## Features

Filled in once the MVP scope is agreed. One row per feature, linked to its page under `project/features/`.

| Feature | State | Design | PR | Notes |
|---------|-------|--------|----|-------|
| [M0 Engineering platform](../features/m0-engineering-platform/) | done | agreed | ARCHI-24, ARCHI-33, ARCHI-34, ARCHI-36, ARCHI-37 | PR 1 (hygiene, #26), PR 2 (`build-logic`, versioning, #42), PR 3 (PR pipeline, scanners, #44), PR 4 (main pipeline, release, images, #48), PR 5 (ruleset, settings, CodeRabbit, site pages, #49) merged; checklist done on 2026-10-06: ruleset verified, settings applied, packages public, seed tag `v0.0.0` and first release `v0.1.0`, CodeRabbit installed (recorded by ARCHI-76) |
| [M1 Agent pipeline completion](../features/m1-agent-pipeline/) | done | agreed | ARCHI-38, ARCHI-39, ARCHI-40, ARCHI-41, ARCHI-42 | PR 1–5 merged (#50, #51, #52, #53, #54): span model, graph, publisher, metrics, formatter; acceptance run 6 000 000 spans in 600 s; B1 closed by `topics.ignore` (ARCHI-77); B4 closed by microsecond latency end to end (ARCHI-78 to ARCHI-81) |
| [M2 Control plane ingestion and storage](../features/m2-control-plane-storage/) | done | agreed | ARCHI-26, ARCHI-28, ARCHI-29 | schema and stores, ingestion, current graph and retention |
| [M3 Query API](../features/m3-query-api/) | done | agreed | ARCHI-30, ARCHI-31 | document and generator; scopes, agents, graph, services, snapshot history; typed problem details; reference page |
| [M4 Service map UI](../features/m4-service-map/) | done | agreed · [UI design](../features/ui-design/) in review | ARCHI-25, ARCHI-32, ARCHI-43, ARCHI-44, ARCHI-45, ARCHI-46 | PR 1–5 merged (#39, #55, #56, #58, #59): `architrace-ui` scaffold, Gradle and SPA integration, typed client, UI gate, Projects list, shell, routing, Service map with React Flow and ELK, lenses, context rail, find, time, selection in the URL, scope switcher, namespace filter; follow-up B1 from the real-data round; B3 delivered as the Platform chip (ARCHI-75) |
| [M5 Drift](../features/m5-drift/) | done | agreed | ARCHI-48, ARCHI-49 | PR 1 (#61: diff domain, algorithm, `diff/environments` and `diff/timeline` endpoints) and PR 2 (#62: Drift screen with modes, sides in the URL, counters, grouped list, map overlays, rail, user guide) merged; the changed-node semantics per mode awaits the maintainer's confirmation |
| [M6 Architecture rules](../features/m6-architecture-rules/) | done | agreed · blast radius added 2026-10-05 (ARCHI-50) | ARCHI-53 to ARCHI-63 | eleven pull requests (#66–#70, #75–#80), one after the other: the rule engine and the seven rules (ARCHI-53 to ARCHI-55), the `finding` table (ARCHI-56), evaluation after every ingested snapshot (ARCHI-57), the `findings` and `impact` endpoints (ARCHI-58), counts on the scope summaries (ARCHI-59), the Findings screen (ARCHI-60), badges on the Projects list, the map and the rail (ARCHI-61), the impact lens with the rail card (ARCHI-62), the user guide (ARCHI-63); B2 delivered as the eighth rule `DataStreamWithoutProducer` (ARCHI-72); B3 in the control plane: platform hosts labelled (ARCHI-73) and known to `UnknownExternal` (ARCHI-74) and grouped on the map (ARCHI-75) |
| [M7 Packaging and demo](../features/m7-packaging-demo/) | done | agreed | ARCHI-36, ARCHI-64 to ARCHI-67 | images with M0 PR 4; the demo stack `demo/` with two environments, broker, database and external host and the Docker demo guide (ARCHI-64, #81), the repository pointed at it (ARCHI-65, #82), `otel-test-app` removed (ARCHI-66, #83), the deployment guide with the Spring Boot instrumentation section, B5 and B7 (ARCHI-67, #90) |

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
| 2026-10-04 | M0 PR 5 merged (#49): second `main` run green, both images pushed to GHCR; M0 complete up to the maintainer checklist |
| 2026-10-04 | M1 PR 1: `SpanRecord` with sealed peers, attribute mapping over current and legacy conventions, normaliser, configuration v2 with `dry-run` and `--prop`, agent tests without reflection (ARCHI-38) |
| 2026-10-04 | M1 PR 1 merged (#50) |
| 2026-10-04 | M1 PR 2: sealed `GraphNode` with ADR 0007 ids, edge builder with pending index and TTL, logarithmic latency histogram, `GraphWindow` and immutable `GraphSnapshot`, bounded span queue and single-owner worker; legacy resolvers removed (ARCHI-39) |
| 2026-10-04 | M1 PR 3: snapshot → protobuf mapping, bounded snapshot queue, control plane session with registration, publishing, acks and heartbeats, supervisor that reconnects, `graph_batch` removed from the contract, server code removed from the agent, end-to-end runtime test (ARCHI-40) |
| 2026-10-04 | M1 PR 4: Micrometer meters over the pipeline counters, Prometheus `/metrics` and `/health` on `metrics.port`, rate-limited drop reporting, load test task (ARCHI-41) |
| 2026-10-04 | M1 PR 2 merged (#51) |
| 2026-10-04 | M1 PR 5: formatter on for the agent, ten-minute acceptance run recorded, defect inventory A1–A12 closed (ARCHI-42) |
| 2026-10-04 | M4 PR 2: Projects list with environment and cluster facets, text filter, grouping and URL state, shell with navigation rail and theme, scope page placeholder, `react-router` (ARCHI-43) |
| 2026-10-04 | M1 PR 5 merged (#54): M1 complete |
| 2026-10-04 | M4 PR 2 merged (#55) |
| 2026-10-04 | M4 PR 3: Service map of a scope with React Flow and the ELK layered layout, node cards by type, edges by kind and health, node-type chips in the URL, legend, minimap, decision taken on a 300-node fixture (ARCHI-44) |
| 2026-10-04 | M4 PR 4: lenses, context rail with scope, node and dependency panels, find-in-map, time selector, selection and time in the URL, keyboard selection, copy link (ARCHI-45) |
| 2026-10-04 | M4 PR 5: scope switcher in the breadcrumb, namespace filter, M1 and M2 pages brought up to date (ARCHI-46) |
| 2026-10-04 | M4 PR 4 re-opened from `main` and merged (#58); M4 PR 5 merged (#59): M4 complete |
| 2026-10-04 | First real-data test round: Architrace unchanged against a private eight-service stack with Kafka, Kafka Streams, a Debezium outbox and Redis; backlog B1–B7 recorded in Requirements §9 (ARCHI-47) |
| 2026-10-04 | M5 PR 1: drift domain and `GraphDiffer`, `DriftQuery`, environment and timeline diff endpoints in the Query API, reference and feature pages (ARCHI-48) |
| 2026-10-04 | M5 PR 2: Drift screen with environment and timeline modes, both sides in the URL, counters, grouped list, map overlays with ghosts, context rail sentences, guide page (ARCHI-49) |
| 2026-10-04 | M5 PR 1 merged (#61) and M5 PR 2 merged (#62): M5 complete |
| 2026-10-05 | Maintainer: blast radius added to M6 as an impact query, a map lens, a rail card and the rule `WideBlastRadius` (ARCHI-50) |
| 2026-10-05 | M6 PR 1: rule engine with `CyclicDependency`, `SharedDatabase` and `UnknownExternal` in the control plane (ARCHI-53) |
| 2026-10-05 | M6 PR 2: blast radius analysis `ImpactAnalysis` with the rule `WideBlastRadius` (ARCHI-54) |
| 2026-10-05 | M6 PR 3: the rules `CrossDomainCoupling`, `FanInHub` and `LongSyncChain` complete the set of seven (ARCHI-55) |
| 2026-10-05 | M6 PR 4: the `finding` table and `JdbcFindingStore` (ARCHI-56) |
| 2026-10-05 | M6 PR 5: rules evaluated after every ingested snapshot, bounded per scope (ARCHI-57) |
| 2026-10-05 | M6 PR 6: `findings` and `impact` endpoints of the Query API (ARCHI-58) |
| 2026-10-05 | M6 PR 7: finding counts per severity on the scope summaries (ARCHI-59) |
| 2026-10-05 | M6 PR 8: the Findings screen with rule groups, filters, evidence and the rail (ARCHI-60) |
| 2026-10-05 | M6 PR 9: finding badges on the Projects list, the map nodes and the map rail (ARCHI-61) |
| 2026-10-05 | M6 PR 10: the impact lens with the card "If X fails" (ARCHI-62) |
| 2026-10-05 | M6 PR 11: user guide and documentation close-out; M6 complete (ARCHI-63) |
| 2026-10-05 | M7 PR 1: the demo stack `demo/` on the published images, validated end to end, with the Docker demo guide (ARCHI-64) |
| 2026-10-05 | M7 PR 2: README, agent instructions, security policy and the pages point at `demo/` (ARCHI-65) |
| 2026-10-05 | M7 PR 3: `otel-test-app` removed; Snyk and Dependabot follow `demo/` (ARCHI-66) |
| 2026-10-05 | M7 PR 4: deployment guide, agent health check on `/health`, B5 to B7 closed; M7 complete (ARCHI-67) |
| 2026-10-06 | README badges: tests, version, quality and stack versions, published by the `main` pipeline (ARCHI-71) |
| 2026-10-06 | M6 follow-up: the rule `DataStreamWithoutProducer` closes B2 from the real-data round (ARCHI-72) |
| 2026-10-06 | B3 part 1: `architrace.topology.platform-hosts` labels the platform hosts `category=platform` in every served graph (ARCHI-73) |
| 2026-10-06 | B3 part 2: `UnknownExternal` treats the platform hosts as known (ARCHI-74) |
| 2026-10-06 | B3 part 3: the map keeps the platform hosts off by default and groups them into one node behind the Platform chip (ARCHI-75); B3 closed |
| 2026-10-06 | First release `v0.1.0`: seed tag `v0.0.0`, pipeline tag and GitHub release with both jars, images `0.1.0` and `latest`; repository settings applied, GHCR packages public, CodeRabbit installed (ARCHI-76) |
| 2026-10-06 | B1 from the real-data round: the agent hides Kafka Streams internal topics through `topics.ignore` (ARCHI-77) |
| 2026-10-06 | B4 from the real-data round: latency in microseconds from the agent histogram through the contract and the control plane to the Query API, which reports fractional milliseconds and the map formats (ARCHI-78 to ARCHI-81); B4 closed |
| 2026-10-06 | `build-logic` JVM 24 pin dropped: Gradle 9.8 embeds Kotlin 2.4.10, which targets JVM 25 (ARCHI-82) |
| 2026-10-06 | Operator documentation: README front page, step-by-step installation guide with Kubernetes manifests, instrumentation guide for the Java agent and the Spring Boot starter (ARCHI-83) |
