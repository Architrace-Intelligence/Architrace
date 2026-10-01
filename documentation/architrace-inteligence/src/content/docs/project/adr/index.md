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
