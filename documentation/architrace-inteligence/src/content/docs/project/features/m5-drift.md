---
title: M5. Drift
description: What differs between two environments, or between two points in time of one environment.
---

Status: design agreed · Order: 6 · Requirements: F9

## Goal

Answer "what is different between DEV and PROD" and "what changed in PROD since yesterday's
release" with a precise, explainable list and a highlighted map.

## Scope

In: diff domain model, environment diff, timeline diff, API endpoints, drift view and map
highlighting.

Out: alerting on drift, scheduled drift reports (post-MVP).

## Design

### Domain

```
TopologyDiff(left: GraphRef, right: GraphRef,
             nodesAdded, nodesRemoved, nodesChanged: List<NodeChange>,
             edgesAdded, edgesRemoved)
NodeChange(nodeId, versionsBefore, versionsAfter, deploymentsBefore, deploymentsAfter)
GraphRef(environment, at)
```

- Nodes are compared by their environment-independent id
  ([ADR 0007](../../adr/0007-topology-model/)); edges by `(sourceId, targetId, kind)`.
- "Changed" means the set of versions or deployments differs. Edge metrics are never part of
  the diff: traffic volume is not drift.
- Complexity is linear in nodes and edges; graphs are loaded through the same
  `currentGraph` query used everywhere.

### Two questions, one algorithm

| Question | Left | Right |
|----------|------|-------|
| Environment drift | `graph(DEV, at)` | `graph(STAGE, at)` |
| Release drift | `graph(PROD, t1)` | `graph(PROD, t2)` |

### API

| Method and path | Returns |
|-----------------|---------|
| `GET /diff/environments?left=DEV&right=STAGE&at=` | `TopologyDiff` |
| `GET /diff/timeline?environment=PROD&from=&to=` | `TopologyDiff` between the two points |

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

1. Diff domain and algorithm with fixture tests.
2. API endpoints, contract tests, OpenAPI update.
3. Drift page, map overlay, user guide page.
