---
title: Decision log
description: Architecture decision records, written only after the maintainer has agreed a decision.
---

An ADR captures one decision: the context that forced it, the decision itself, its consequences
and the alternatives that were rejected. Records are immutable once merged; a change of mind
produces a new record that supersedes the old one.

| ID   | Title                                                        | Status   | Date       |
|------|--------------------------------------------------------------|----------|------------|
| 0001 | [PostgreSQL with Liquibase for the control plane store](./0001-postgresql-liquibase/) | accepted | 2026-10-01 |
| 0002 | [CodeRabbit as the independent AI reviewer](./0002-coderabbit-ai-review/) | accepted | 2026-10-01 |
| 0003 | [Maintainer-authored pull requests and the merge gate](./0003-review-identity-and-merge-gate/) | accepted | 2026-10-01 |
| 0004 | [React and TypeScript single-page UI served by the control plane](./0004-react-typescript-ui/) | accepted | 2026-10-01 |
| 0005 | [Monorepo module layout and dependency rules](./0005-module-layout/) | accepted | 2026-10-01 |
| 0006 | [Semantic versioning from Conventional Commits and the release flow](./0006-versioning-and-release-flow/) | accepted | 2026-10-01 |
| 0007 | [Topology model: identity, snapshots and time](./0007-topology-model/) | accepted | 2026-10-01 |
| 0008 | [Contract-first APIs: protobuf for agents, OpenAPI for the Query API](./0008-contract-first-apis/) | accepted | 2026-10-01 |
| 0009 | [Spring Data JDBC for control plane persistence](./0009-spring-data-jdbc/) | accepted | 2026-10-01 |
| 0010 | [Pin Spring Boot and Spring gRPC to GA releases](./0010-spring-boot-ga/) | accepted | 2026-10-01 |

## Template

```markdown
---
title: NNNN. Short imperative title
description: One sentence.
---

Status: proposed | accepted | superseded by NNNN
Date: YYYY-MM-DD

## Context
## Decision
## Consequences
## Alternatives considered
```
