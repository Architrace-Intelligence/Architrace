---
title: M5. Drift
description: What differs between two environments, or between two points in time of one environment.
---

Status: in progress (PR 1 delivered as ARCHI-48, PR 2 open) · Order: 6 · Requirements: F9

## Goal

Answer "what is different between DEV and PROD" and "what changed in PROD since yesterday's
release" with a precise, explainable list and a highlighted map.

## Scope

In: diff domain model, environment diff, timeline diff, API endpoints, drift view and map
highlighting.

Out: alerting on drift, scheduled drift reports (post-MVP), the "makes the database shared"
sentence of the context rail (it previews a rule; it comes with the rules of
[M6](../m6-architecture-rules/)).

## Design

Visual design and interaction: [UI design](../ui-design/) (interactive prototypes).

### Domain

Package `drift` of the control plane, data only:

```
TopologyDiff(left: GraphRef, right: GraphRef,
             nodesAdded: List<TopologyNode>, nodesRemoved: List<TopologyNode>,
             nodesChanged: List<NodeChange>,
             edgesAdded: List<EdgeKey>, edgesRemoved: List<EdgeKey>)
NodeChange(before: TopologyNode, after: TopologyNode)
GraphRef(scope, at)
EdgeKey(sourceId, targetId, kind)          # topology package, shared with the graph merger
DiffMode = ENVIRONMENTS | TIMELINE
```

- A side is a **scope at a point in time** (`GraphRef`): the scope model of
  [ADR 0007](../../adr/0007-topology-model/) is project × environment × cluster, so an
  environment diff compares two scopes of one project (another environment, or another
  cluster of the same environment) and a timeline diff compares one scope at two instants.
- Nodes are compared by their environment-independent id; edges by `EdgeKey`
  (`sourceId`, `targetId`, `kind`). "Added" means only on the right, "removed" only on the
  left; the lists carry the node as seen on its own side, so a removed node can be drawn as a
  ghost.
- **Changed** depends on the mode. In `ENVIRONMENTS` a node is changed when its set of
  versions differs. In `TIMELINE` it is changed when its set of versions or its set of
  deployments differs. Deployments (cluster, namespace) differ between environments by
  construction, comparing them there would mark every node; this is the refinement proposed
  on the UI design page, implemented as the working assumption of ARCHI-48 and awaiting the
  maintainer's confirmation. Both sides' versions and deployments are reported in every mode.
- Edge metrics are never part of the diff: traffic volume is not drift.
- `GraphDiffer.diff(left, right, mode)` is a pure function, linear in nodes and edges; both
  graphs come from the same `currentGraph` query the map uses. `DriftQuery` is the service
  behind the endpoints: it resolves the two graphs and validates `from ≤ to`.

### Two questions, one algorithm

| Question | Left | Right | Mode |
|----------|------|-------|------|
| Environment drift | `graph(webshop/DEV/k8s-dev, at)` | `graph(webshop/PROD/k8s-prod-eu1, at)` | `ENVIRONMENTS` |
| Release drift | `graph(webshop/PROD/k8s-prod-eu1, t1)` | `graph(webshop/PROD/k8s-prod-eu1, t2)` | `TIMELINE` |

### API

The right side is the scope in the path, so both endpoints sit next to the graph of a scope
([Query API](../../../reference/query-api/#drift)):

| Method and path | Returns |
|-----------------|---------|
| `GET /scopes/{project}/{environment}/{cluster}/diff/environments?leftEnvironment=&leftCluster=&at=` | `TopologyDiff` of another scope of the project (left) against this scope (right), both at `at` (default now) |
| `GET /scopes/{project}/{environment}/{cluster}/diff/timeline?from=&to=` | `TopologyDiff` of this scope at `from` (left) against `to` (right, default now) |

`from` after `to` answers `400 invalid-query`; a side without a registered agent answers
`404 scope-not-found`; a missing required parameter is a framework `400` without a type.
`NodeChange` is flattened on the wire (`id`, `type`, `name`, `versionsBefore`,
`versionsAfter`, `deploymentsBefore`, `deploymentsAfter`); edges without metrics are
`EdgeRef`.

### UI

- Drift page: pick mode (environments or timeline), pick sides, see summary counters and
  grouped lists (services, data stores, topics, externals; added, removed, changed).
- "Show on map" opens the service map with overlays: added green, removed red (ghost nodes
  from the left side), changed amber; the legend explains colours.
- Deep links carry both sides so a drift view can be shared in a review.

## Acceptance criteria

- Diff of a graph with itself is empty; diff of the demo environments lists the intended
  differences exactly (fixture-based test).
- Version-only changes appear as changed nodes, not as removed plus added.
- Drift page and map overlay work for both modes with URL state.

## Delivery plan

1. **PR 1 (ARCHI-48): diff domain, algorithm and API.** `EdgeKey` promoted to a public
   record of the topology package (the merger and the differ share one edge identity);
   `drift` package with `GraphRef`, `NodeChange`, `TopologyDiff`, `DiffMode`, `GraphDiffer`
   and `DriftQuery`; `drift.web` with `DriftController` (generated `DriftApi`) and
   `DriftModels`; the shared `ApiModels` opened for the scope, node and time mappings; two
   endpoints and four schemas in the OpenAPI document. Tests: `GraphDifferTest` (identity,
   one-sided nodes and edges in order, version change as changed node, mode semantics,
   metrics ignored, edge kinds distinct), `DriftQueryTest` (both modes over the in-memory
   stores, validation, unknown sides), `DriftApiTest` (strict JSON of both endpoints, typed
   problems). Patterns: pure function over immutable records for the algorithm; set
   difference on ids and edge keys; exhaustive `switch` on the mode without `default`; the
   right side as the REST resource and the left side as parameters, so drift lives under the
   scope it describes.
2. **PR 2 (ARCHI-49): Drift screen.** Route `/scopes/{project}/{environment}/{cluster}/drift`
   with the mode, the left side, the time bounds, the view (list or map) and the selection in
   the URL; segmented controls for mode and view; five counters; grouped list with
   "Show on map"; map view reusing the service map with drift overlays (added, removed as
   ghost, changed) and a drift legend; context rail with the comparison, the selected
   difference and deterministic summary sentences; "Drift" in the navigation rail; user guide
   page.
