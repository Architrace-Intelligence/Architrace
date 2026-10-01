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
| 1 | MVP scope agreement                     | in progress | Candidate scope in [Requirements §6](../requirements/#6-proposed-mvp-scope) |
| 2 | Design (architecture, ADRs, features)   | todo        | Starts after stage 1 is agreed                         |
| 3 | Implementation (feature by feature)     | todo        |                                                        |
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
| ADR folder and first ADRs                         | review | ADR 0001–0004, PR #22                      |
| Architecture document                             | todo  | first version in design stage               |
| Repository hygiene (templates, stale files)       | todo  | requirements §4.6                           |

## Features

Filled in once the MVP scope is agreed. One row per feature, linked to its page under `project/features/`.

| Feature | State | Design | PR | Notes |
|---------|-------|--------|----|-------|
|         |       |        |    |       |

## History

| Date       | Event                                                        |
|------------|--------------------------------------------------------------|
| 2026-10-01 | Project analysed, requirements and process documents drafted |
| 2026-10-01 | Automation token issued and verified; documentation moved to the site |
| 2026-10-01 | Decisions recorded: storage, AI reviewer, review identity, ticket numbering, copyright |
| 2026-10-01 | UI stack decided: React + TypeScript (ADR 0004) |
