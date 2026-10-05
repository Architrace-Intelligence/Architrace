---
title: M6. Architecture rules
description: Deterministic checks that turn a topology into findings with severity and evidence, and the blast radius of any node.
---

Status: done on 2026-10-05 (ARCHI-53 to ARCHI-63) · Order: 7 · Requirements: F10

## Goal

Architects get immediate, explainable feedback on structural problems without any LLM: the
same graph always yields the same findings. The same engine answers "what breaks if this
fails" for any node of the graph.

## Scope

In: rule engine, eight rules with configuration, evaluation after ingestion, persistence,
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
| CrossDomainCoupling | a service with sync edges into services of more than N other domains (the domain is parsed from `service:{domain}/{name}`) | 3 domains | medium |
| FanInHub | a service with inbound sync degree above N (direct callers, a coupling hotspot) | 8 | medium |
| LongSyncChain | the longest sync path from an entry node (a node nobody calls), over the condensation of the strongly connected components: a cycle collapses to its smallest member and has its own finding, a database or external host counts as the last hop | 5 hops | medium |
| UnknownExternal | an external node not in the allowlist | empty allowlist | low |
| DataStreamWithoutProducer | a topic with `consume` edges but no `publish` edge in the observed traces, usually an outbox filled by change data capture (B2) | none | low |

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
  evidence; the path runs from the node to the subject, so `distance` is its length in hops.
  `delayed` is one hop further through `publish` and `consume` edges, minus the nodes already
  impaired: the consumers of every topic the subject or an impaired service publishes to,
  with the topic on the path (`distance` therefore counts the asynchronous passage as two
  hops), and the consumers of the subject itself when it is a topic. Both lists are ordered
  by distance, then id; among equally short paths the lexicographically smallest wins. An
  unknown node id yields no analysis (an empty `Optional` in the engine, a 404 problem in the
  API).
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

- Findings page: findings grouped by rule in severity order, severity chips and a rule select
  in the URL, rows that expand to the detail, the evidence and "Show on map" (the map with the
  first subject selected), the allowlist line for unknown externals, the rules that pass, a
  rail with the totals and every rule's status; the Projects list carries the counts per
  severity, the map a badge with the finding count on every subject node and the findings of
  the selected node in its rail.
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

Delivered as eleven pull requests against `main`, one at a time, each at most twelve files:

1. Engine, `CyclicDependency`, `SharedDatabase`, `UnknownExternal`, configuration, tests
   (ARCHI-53).
2. `ImpactAnalysis` with `WideBlastRadius` and its thresholds (ARCHI-54).
3. `CrossDomainCoupling`, `FanInHub`, `LongSyncChain`, the shared
   `StronglyConnectedComponents` (ARCHI-55).
4. The `finding` table, `FindingStore`, `JdbcFindingStore` (ARCHI-56).
5. Evaluation after every ingested snapshot, bounded per scope, Spring wiring, configuration
   reference (ARCHI-57).
6. Findings and impact endpoints, OpenAPI, Query API reference (ARCHI-58).
7. Finding counts on the scope summaries (ARCHI-59).
8. Findings screen (ARCHI-60).
9. Finding badges on the Projects list, the map and the map rail (ARCHI-61).
10. Impact lens and the rail card (ARCHI-62).
11. User guide and documentation close-out (ARCHI-63).
12. `DataStreamWithoutProducer`, the B2 insight from the real-data round (ARCHI-72).

## Open points

From the real-data round ([Requirements §9](../../requirements/#9-backlog-from-the-first-real-data-test-round)):

- B3: a platform category in the external allowlist (feature-flag server, config server) so
  these hosts are grouped on the map and skipped by `UnknownExternal`.

Candidates for a later release, all computable from the graph the agent already reports:
`ChattyDependency` (a sync edge with a call count far above the median of the graph),
`GodService` (a service whose total degree, domains touched and databases owned all exceed
their thresholds), `NanoService` (a service with a single caller, no database and at most one
dependency), `OrphanService` (no edge in either direction).
