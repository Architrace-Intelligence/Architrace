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
| 3 | Implementation (feature by feature)     | in progress | M0 started                                             |
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
| Commit convention and automated versioning        | todo  | needs agreement (requirements §5.3)         |
| CI: single PR pipeline with quality gates         | todo  | needs agreement (requirements §5.2)         |
| CI: AI code review                                | todo  | CodeRabbit (ADR 0002); app install pending  |
| CI: security scanning (Snyk, OWASP, CodeQL, …)    | todo  | needs agreement (requirements §5.5)         |
| CD: main pipeline, release, images                | todo  | needs agreement (requirements §5.2)         |
| Branch ruleset on `main`                          | todo  | shape agreed in ADR 0003; applied in M0     |
| ADR folder and first ADRs                         | done  | ADR 0001–0010 accepted                      |
| Architecture page                                 | done  | current and target views                    |
| Repository hygiene (templates, stale files)       | todo  | requirements §4.6                           |

## Features

Filled in once the MVP scope is agreed. One row per feature, linked to its page under `project/features/`.

| Feature | State | Design | PR | Notes |
|---------|-------|--------|----|-------|
| [M0 Engineering platform](../features/m0-engineering-platform/) | in progress | agreed | | PR 1 of 5: hygiene |
| [M1 Agent pipeline completion](../features/m1-agent-pipeline/) | todo | agreed | | |
| [M2 Control plane ingestion and storage](../features/m2-control-plane-storage/) | todo | agreed | | |
| [M3 Query API](../features/m3-query-api/) | todo | agreed | | |
| [M4 Service map UI](../features/m4-service-map/) | todo | agreed | | |
| [M5 Drift](../features/m5-drift/) | todo | agreed | | |
| [M6 Architecture rules](../features/m6-architecture-rules/) | todo | agreed | | |
| [M7 Packaging and demo](../features/m7-packaging-demo/) | todo | agreed | | compose stack needed from M2 |

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
