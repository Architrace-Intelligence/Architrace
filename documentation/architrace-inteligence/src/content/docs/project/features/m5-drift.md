---
title: M5. Drift
description: What differs between two environments, or between two points in time of one environment.
---

Status: done (PR 1 ARCHI-48 merged as #61, PR 2 ARCHI-49 merged as #62) · Order: 6 · Requirements: F9

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

- Drift page at `/scopes/{project}/{environment}/{cluster}/drift`: the scope in the path is
  the right side. Mode (environments or timeline), left side (`left`, `leftCluster`), time
  (`at`, or `from` and `to`), view (`list` or `map`) and selection (`node`) live in the URL;
  a missing left side or `from` is filled with a default (another environment of the project,
  one day ago) by a replace navigation, so a copied link always carries both sides.
- Five counters, then a grouped list (services, data stores, data streams, external,
  dependencies; added, removed, changed) with "Show on map", or the map view: the right
  side's graph plus the removed nodes and edges as ghosts, overlays from the diff (added
  green, removed red dashed, changed amber with both versions on the card), a drift legend.
- The context rail states the comparison, describes the selected node and turns the diff into
  deterministic sentences. User guide: [Comparing environments and releases](../../../guides/drift/).

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
2. **PR 2 (ARCHI-49): Drift screen.** `drift/model.ts` holds the URL state (`parseDriftState`,
   `toDriftParams`), the defaults (`defaultLeft` prefers another environment, `defaultFrom` is
   one day ago), the side labels, the counters, the grouped rows, the map overlay, the union
   graph with ghosts and the summary sentences, all as pure functions over the `TopologyDiff`
   of the API and the right side's `TopologyGraph`. `DriftPage` composes the shell, the
   pickers (`ScopeSwitcher` for the right side, a select for the left, swap, the time
   selectors), `DriftList`, the map and `DriftPanel`. The service map gained an optional
   overlay (`ChangeKind` per node and edge, a subtitle per changed node) and a legend variant,
   the node card a flag glyph, the time selector a prefix, a live label and a no-live mode.
   Tests: `model.test.ts` (round trip, defaults, labels, groups, overlay, union, sentences,
   selection) and `DriftPage.test.tsx` (default left side and request, left switch and swap
   through the URL, map view with ghosts, flags, legend and selection, timeline mode with
   the time selectors, single-scope project, aligned sides, failing control plane and graph).
   Patterns: the URL as the only client state with declarative `Navigate` for defaults;
   derived server state through `queryOptions` factories and `enabled` flags; the diff
   rendered by the same map through an overlay instead of a second map component; pure
   functions for every sentence the rail says, so an agent can later cite them.
