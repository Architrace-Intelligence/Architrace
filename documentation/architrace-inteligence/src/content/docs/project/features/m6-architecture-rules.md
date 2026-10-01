---
title: M6. Architecture rules
description: Deterministic checks that turn a topology into findings with severity and evidence.
---

Status: design agreed · Order: 7 · Requirements: F10

## Goal

Architects get immediate, explainable feedback on structural problems without any LLM: the
same graph always yields the same findings.

## Scope

In: rule engine, six rules with configuration, evaluation after ingestion, persistence,
findings API, findings view and map badges.

Out: custom rule authoring in the UI, LLM-generated insights (C12, post-MVP), suppression
workflow (simple allowlists only).

## Design

Visual design and interaction: [UI design](../ui-design/) (interactive prototypes).

### Engine

```
sealed interface ArchitectureRule permits CyclicDependency, CrossDomainCoupling, FanInHub,
                                          LongSyncChain, SharedDatabase, UnknownExternal
record Finding(ruleId, severity, environment, subjectNodeIds, title, detail, evidence, evaluatedAt)
RuleEngine.evaluate(TopologyGraph, RulesConfig) -> List<Finding>
```

Rules are pure functions over an immutable `TopologyGraph`; configuration is a record built
from Spring properties `architrace.rules.*`.

### Rules

| Rule | Detects | Default threshold | Severity |
|------|---------|-------------------|----------|
| CyclicDependency | strongly connected components over `sync` edges (Tarjan) | any cycle | high |
| SharedDatabase | a database node with more than one service depending on it | 2 services | high |
| CrossDomainCoupling | a service with sync edges into more than N other domains | 3 domains | medium |
| FanInHub | a service with inbound sync degree above N (single point of failure candidate) | 8 | medium |
| LongSyncChain | a simple sync path longer than N hops | 5 | medium |
| UnknownExternal | an external node not in the allowlist | empty allowlist | low |

### Evaluation and storage

- After every ingested snapshot the engine runs on the current graph of that environment; the
  previous findings of the environment are replaced in one transaction (`finding` table:
  environment, rule_id, severity, subject_ids, title, detail, evidence jsonb, evaluated_at).
- Evaluation is bounded: at most once per environment per interval even if many agents report.

### API and UI

- `GET /environments/{env}/findings?severity=&rule=` returns findings; counts per severity
  are included in the environment list.
- Findings page: table grouped by rule, severity badges, "show on map" highlights the subject
  nodes; the map shows a badge with the finding count on affected nodes.

## Acceptance criteria

- Every rule has fixture tests with positive and negative graphs; thresholds are configurable
  and documented on the configuration reference page.
- The demo topology produces at least a cycle and a shared database finding.
- Findings refresh within one snapshot interval after a topology change.

## Delivery plan

1. Engine, `CyclicDependency`, `SharedDatabase`, `UnknownExternal`, tests.
2. Remaining rules, persistence, evaluation trigger, API, OpenAPI update.
3. Findings page, map badges, user guide page.
