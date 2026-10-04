---
title: M6. Architecture rules
description: Deterministic checks that turn a topology into findings with severity and evidence, and the blast radius of any node.
---

Status: design agreed, blast radius added on 2026-10-05 · Order: 7 · Requirements: F10

## Goal

Architects get immediate, explainable feedback on structural problems without any LLM: the
same graph always yields the same findings. The same engine answers "what breaks if this
fails" for any node of the graph.

## Scope

In: rule engine, seven rules with configuration, evaluation after ingestion, persistence,
findings API, findings view and map badges, blast radius analysis with its API, map lens and
rail card.

Out: custom rule authoring in the UI, LLM-generated insights (C12, post-MVP), suppression
workflow (simple allowlists only), rules on code-level smells that the graph cannot see
(chatty pairs, god and nano services are candidates for a later release, see open points).

## Design

Visual design and interaction: [UI design](../ui-design/) (interactive prototypes).

### Engine

```
sealed interface ArchitectureRule permits CyclicDependency, CrossDomainCoupling, FanInHub,
                                          LongSyncChain, SharedDatabase, UnknownExternal,
                                          WideBlastRadius
record Finding(ruleId, severity, scope, subjectNodeIds, title, detail, evidence, evaluatedAt)
RuleEngine.evaluate(TopologyGraph, RulesConfig) -> List<Finding>
```

Rules are pure functions over an immutable `TopologyGraph`; configuration is a record built
from Spring properties `architrace.rules.*`. The unit of evaluation is the scope
(project × environment × cluster, [ADR 0007](../../adr/0007-topology-model/)), the same
graph the map and the drift use.

### Rules

| Rule | Detects | Default threshold | Severity |
|------|---------|-------------------|----------|
| CyclicDependency | strongly connected components over `sync` edges (Tarjan) | any cycle | high |
| SharedDatabase | a database node with more than one service depending on it | 2 services | high |
| WideBlastRadius | a node whose failure impairs more than N % of the services of the scope (see Blast radius) | 50 %, at least 3 services | high |
| CrossDomainCoupling | a service with sync edges into more than N other domains | 3 domains | medium |
| FanInHub | a service with inbound sync degree above N (direct callers, a coupling hotspot) | 8 | medium |
| LongSyncChain | a simple sync path longer than N hops | 5 | medium |
| UnknownExternal | an external node not in the allowlist | empty allowlist | low |

### Blast radius

The blast radius of a node is the set of nodes whose correct operation depends on it,
directly or transitively. It is a query on any node, not only a rule, because an architect
asks it about a service that is about to be deployed, not only about one that crossed a
threshold.

```
Impact(subject: TopologyNode, at,
       impaired: List<ImpactedNode>, delayed: List<ImpactedNode>,
       services: int, servicesTotal: int)
ImpactedNode(node, distance, path: List<String>)
ImpactAnalysis.of(TopologyGraph, nodeId) -> Impact
```

Propagation follows the data, edge kind by edge kind:

| The subject is | Impaired (fails or errors) | Delayed (keeps running, data arrives late) |
|----------------|----------------------------|--------------------------------------------|
| a service or an external host | every caller over a `sync` edge, transitively | consumers of the topics the impaired services publish to |
| a database | every service with a `sync` edge to it, then their callers | consumers of the topics those services publish to |
| a topic | its producers (a publish is on the request path), then their callers | its consumers |

- `impaired` is the reverse reachability over `sync` edges from the subject (a reverse
  breadth-first search), each node with its distance in hops and one shortest path as
  evidence. `delayed` is one hop further through `publish` and `consume` edges, minus the
  nodes already impaired. Both lists are ordered by distance, then id.
- `services` counts the impaired service nodes, `servicesTotal` the service nodes of the
  graph; the share is theirs. Delayed nodes are reported but never counted as failures:
  asynchronous decoupling is exactly what the analysis must give credit for.
- Linear in nodes and edges; the same graph query as everywhere (`currentGraph`), so the
  analysis works at any point in time with `at`.
- `WideBlastRadius` is the rule on top: the analysis runs for every service, database, topic
  and external node of the graph; a subject whose impaired share exceeds the threshold raises
  one finding with the impaired list as evidence. The absolute minimum of three services
  keeps small graphs quiet.

### Evaluation and storage

- After every ingested snapshot the engine runs on the current graph of that scope; the
  previous findings of the scope are replaced in one transaction (`finding` table: scope
  columns, rule_id, severity, subject_ids, title, detail, evidence jsonb, evaluated_at).
- Evaluation is bounded: at most once per scope per interval even if many agents report.
- The impact analysis is not stored: it is computed on request from the current graph, like
  the drift.

### API and UI

| Method and path | Returns |
|-----------------|---------|
| `GET /scopes/{project}/{environment}/{cluster}/findings?severity=&rule=` | the findings of the scope, grouped in the UI by rule; counts per severity join the scope summaries of `GET /scopes` |
| `GET /scopes/{project}/{environment}/{cluster}/impact?node=&at=` | the `Impact` of one node (the id is a query parameter because ids contain `/`) |

- Findings page: table grouped by rule, severity badges, "show on map" highlights the subject
  nodes; the map shows a badge with the finding count on affected nodes.
- Impact lens on the map (`lens=impact` with the selected node in the URL): impaired nodes
  coloured by distance (one hop strong, further hops lighter), delayed nodes hatched,
  everything else dimmed; the context rail gains the card "If *X* fails": impaired services
  out of all, the share, the list by distance with the path of each node. The
  `WideBlastRadius` finding's "show on map" opens this lens on its subject.

## Acceptance criteria

- Every rule has fixture tests with positive and negative graphs; thresholds are configurable
  and documented on the configuration reference page.
- The impact analysis has fixture tests for a chain, a diamond, a database with two clients,
  a topic with producers and consumers, and a node nobody depends on; the subject itself is
  never in its own lists.
- The demo topology produces at least a cycle, a shared database and a wide blast radius
  finding.
- Findings refresh within one snapshot interval after a topology change.
- The impact lens and the rail card work from the URL for every node type.

## Delivery plan

1. Engine, `CyclicDependency`, `SharedDatabase`, `UnknownExternal`, configuration, tests.
2. `ImpactAnalysis` with `WideBlastRadius`, the remaining rules, persistence, evaluation
   trigger, findings and impact endpoints, OpenAPI update, reference pages.
3. Findings page, map badges, impact lens and rail card, user guide page.

## Open points

From the real-data round ([Requirements §9](../../requirements/#9-backlog-from-the-first-real-data-test-round)):

- B2: a data stream with consumers but no producer is usually an outbox filled by change data
  capture; an insight should say so instead of leaving the topic dangling.
- B3: a platform category in the external allowlist (feature-flag server, config server) so
  these hosts are grouped on the map and skipped by `UnknownExternal`.

Candidates for a later release, all computable from the graph the agent already reports:
`ChattyDependency` (a sync edge with a call count far above the median of the graph),
`GodService` (a service whose total degree, domains touched and databases owned all exceed
their thresholds), `NanoService` (a service with a single caller, no database and at most one
dependency), `OrphanService` (no edge in either direction).
