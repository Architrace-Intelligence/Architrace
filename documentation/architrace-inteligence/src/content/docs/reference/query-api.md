---
title: Query API
description: The read-only HTTP API of the control plane, its OpenAPI document and Swagger UI.
---

The control plane serves a read-only HTTP API under `/api/v1`. The OpenAPI 3.1 document is the
contract ([ADR 0008](../../project/adr/0008-contract-first-apis/)); the server interfaces are
generated from it, and so is the TypeScript client of the UI.

- Document: `GET /api/v1/openapi.yaml` (source:
  `architrace-api/src/main/resources/openapi/architrace-query-api.yaml`)
- Swagger UI: `/swagger-ui`

## Endpoints

| Method and path | Returns |
|-----------------|---------|
| `GET /api/v1/scopes` | one summary per scope (project, environment, cluster) with a registered agent, ordered by project, environment and cluster |
| `GET /api/v1/scopes/{project}/{environment}/{cluster}/graph?at=` | the current graph of the scope at `at` (default now): nodes ordered by id, edges ordered by source, target and kind |
| `GET /api/v1/scopes/{project}/{environment}/{cluster}/services?at=` | every service of that graph with its inbound and outbound dependencies, the node at the other end embedded |
| `GET /api/v1/scopes/{project}/{environment}/{cluster}/snapshots?from=&to=&page=&size=` | the snapshot history of the scope, newest first, one page at a time |
| `GET /api/v1/snapshots/{snapshotId}` | one snapshot as the agent reported it, nodes and edges included |
| `GET /api/v1/scopes/{project}/{environment}/{cluster}/diff/environments?leftEnvironment=&leftCluster=&at=` | the drift between another scope of the project (left) and this scope (right), both at `at` (default now) |
| `GET /api/v1/scopes/{project}/{environment}/{cluster}/diff/timeline?from=&to=` | the drift of this scope between `from` (left) and `to` (right, default now) |
| `GET /api/v1/scopes/{project}/{environment}/{cluster}/findings?severity=&rule=` | the findings of the last rule evaluation of the scope, high severity first, optionally filtered by severity or rule id |
| `GET /api/v1/scopes/{project}/{environment}/{cluster}/impact?node=&at=` | the blast radius of one node of the current graph at `at` (default now): impaired and delayed nodes with distance and path |
| `GET /api/v1/agents` | every registered agent with its scope, version, first and last seen time and liveness, ordered by scope and name |

Timestamps are RFC 3339 in both directions (`2026-10-01T12:00:00Z`).

## Scopes and agents

```bash
curl -s http://localhost:8085/api/v1/scopes | jq .
```

```json
[
  {
    "scope": { "project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1" },
    "agents": 2,
    "liveAgents": 2,
    "services": 14,
    "dataStreams": 3,
    "namespaces": 4,
    "lastSnapshotAt": "2026-10-01T12:00:00Z"
  }
]
```

The counts of a scope come from its current graph: the latest snapshot of every agent of the
scope, merged. `lastSnapshotAt` is absent until the first snapshot arrives. An agent is `live`
while fewer than three heartbeat intervals have passed since it was last seen.

## Graph

```bash
curl -s 'http://localhost:8085/api/v1/scopes/webshop/PROD/k8s-prod-eu1/graph?at=2026-10-01T12:00:00Z' | jq .
```

```json
{
  "scope": { "project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1" },
  "at": "2026-10-01T12:00:00Z",
  "nodes": [
    { "id": "db:postgresql/orders", "type": "DATABASE", "name": "orders",
      "versions": [], "deployments": [], "labels": {} },
    { "id": "service:orders", "type": "SERVICE", "name": "orders",
      "versions": ["2.8.1"],
      "deployments": [{ "cluster": "k8s-prod-eu1", "namespace": "orders" }],
      "labels": { "team": "orders" } },
    { "id": "topic:kafka/order-events", "type": "TOPIC", "name": "order-events",
      "versions": [], "deployments": [], "labels": {} }
  ],
  "edges": [
    { "sourceId": "service:orders", "targetId": "db:postgresql/orders", "kind": "SYNC",
      "metrics": { "calls": 24000, "errors": 3, "p50Millis": 3, "p95Millis": 9,
                   "p99Millis": 22, "maxMillis": 140 } },
    { "sourceId": "service:orders", "targetId": "topic:kafka/order-events", "kind": "PUBLISH",
      "metrics": { "calls": 8700, "errors": 0, "p50Millis": 2, "p95Millis": 5,
                   "p99Millis": 9, "maxMillis": 60 } }
  ]
}
```

The graph is the latest snapshot of every agent of the scope whose window ends at or before
`at`, merged: nodes are united by id (versions, deployments and labels are unioned), edges by
source, target and kind (calls and errors summed, latencies at their maximum). Without `at`
the graph is resolved for now. A scope without snapshots answers an empty graph; a scope
without a registered agent answers `404`.

Node ids are stable across scopes and snapshots and may contain `/`. Node types are `SERVICE`,
`DATABASE`, `TOPIC` and `EXTERNAL`. Edge direction follows the data: `SYNC` from the caller
to the callee, `PUBLISH` from the producer to the topic, `CONSUME` from the topic to the
consumer.

## Services

```bash
curl -s http://localhost:8085/api/v1/scopes/webshop/PROD/k8s-prod-eu1/services \
  | jq '.[] | select(.node.id == "service:orders")'
```

```json
{
  "node": { "id": "service:orders", "type": "SERVICE", "name": "orders", "versions": ["2.8.1"],
            "deployments": [{ "cluster": "k8s-prod-eu1", "namespace": "orders" }], "labels": {} },
  "inbound": [
    { "node": { "id": "service:checkout", "type": "SERVICE", "name": "checkout", "versions": ["4.1.0"],
                "deployments": [{ "cluster": "k8s-prod-eu1", "namespace": "checkout" }], "labels": {} },
      "edge": { "sourceId": "service:checkout", "targetId": "service:orders", "kind": "SYNC",
                "metrics": { "calls": 12400, "errors": 37, "p50Millis": 12, "p95Millis": 48,
                             "p99Millis": 90, "maxMillis": 410 } } }
  ],
  "outbound": [
    { "node": { "id": "db:postgresql/orders", "type": "DATABASE", "name": "orders",
                "versions": [], "deployments": [], "labels": {} },
      "edge": { "sourceId": "service:orders", "targetId": "db:postgresql/orders", "kind": "SYNC",
                "metrics": { "calls": 24000, "errors": 3, "p50Millis": 3, "p95Millis": 9,
                             "p99Millis": 22, "maxMillis": 140 } } }
  ]
}
```

One entry per service node of the current graph, ordered by id. `inbound` lists the edges that
end at the service (its callers and the streams it consumes), `outbound` the edges that start
at it (the services, databases and external hosts it calls and the streams it publishes to).
Every entry carries the whole edge and the node at the other end, so the list is self-contained.
There is no per-service resource: ids contain `/`, so a service is picked from the list.

## Snapshot history

```bash
curl -s 'http://localhost:8085/api/v1/scopes/webshop/PROD/k8s-prod-eu1/snapshots?from=2026-10-01T00:00:00Z&size=20' | jq .
curl -s http://localhost:8085/api/v1/snapshots/42 | jq '.nodes | length'
```

```json
{
  "items": [
    { "id": 42, "agentId": 1,
      "scope": { "project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1" },
      "window": { "start": "2026-10-01T11:59:00Z", "end": "2026-10-01T12:00:00Z" },
      "receivedAt": "2026-10-01T12:00:01Z", "nodeCount": 17, "edgeCount": 31 }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

The history lists every stored snapshot of the scope whose window ends between `from` and `to`
(both inclusive, both optional), newest first. `page` is zero-based, `size` is 1 to 200 and
defaults to 50. Summaries carry counts only; `GET /api/v1/snapshots/{snapshotId}` returns the
snapshot with its nodes and edges exactly as the agent reported it. Retention removes old
snapshots, so an id listed earlier can answer `404` later.

## Drift

```bash
curl -s 'http://localhost:8085/api/v1/scopes/webshop/PROD/k8s-prod-eu1/diff/environments?leftEnvironment=DEV&leftCluster=k8s-dev' | jq .
curl -s 'http://localhost:8085/api/v1/scopes/webshop/PROD/k8s-prod-eu1/diff/timeline?from=2026-09-30T18:00:00Z' | jq .nodesChanged
```

```json
{
  "left": { "scope": { "project": "webshop", "environment": "DEV", "cluster": "k8s-dev" },
            "at": "2026-10-01T12:00:00Z" },
  "right": { "scope": { "project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1" },
             "at": "2026-10-01T12:00:00Z" },
  "nodesAdded": [
    { "id": "service:reporting", "type": "SERVICE", "name": "reporting", "versions": ["1.2.0"],
      "deployments": [{ "cluster": "k8s-prod-eu1", "namespace": "reporting" }], "labels": {} }
  ],
  "nodesRemoved": [
    { "id": "db:postgresql/catalog", "type": "DATABASE", "name": "catalog",
      "versions": [], "deployments": [], "labels": {} }
  ],
  "nodesChanged": [
    { "id": "service:search", "type": "SERVICE", "name": "search",
      "versionsBefore": ["2.3.0"], "versionsAfter": ["2.2.1"],
      "deploymentsBefore": [{ "cluster": "k8s-dev", "namespace": "search" }],
      "deploymentsAfter": [{ "cluster": "k8s-prod-eu1", "namespace": "search" }] }
  ],
  "edgesAdded": [
    { "sourceId": "service:reporting", "targetId": "db:postgresql/orders", "kind": "SYNC" }
  ],
  "edgesRemoved": [
    { "sourceId": "service:search", "targetId": "db:postgresql/catalog", "kind": "SYNC" }
  ]
}
```

A diff has a left side and a right side, each a scope at a point in time. Everything
"added" exists only on the right, everything "removed" only on the left, so the diff reads
as "what the right side has that the left side has not". Nodes are matched by their stable
id and listed as they look on their own side; edges are matched by source, target and kind
and listed without metrics, because traffic volume is not drift. Lists are ordered by id, or
by source, target and kind.

`diff/environments` compares the current graphs of two scopes of the same project at `at`
(default now): the right side is the scope in the path, the left side is named by
`leftEnvironment` and `leftCluster`, so another cluster of the same environment is a valid
left side too. A node on both sides is `changed` when its set of versions differs;
deployments differ between environments by construction and are reported but not compared.

`diff/timeline` compares one scope with itself at `from` (left, required) and `to` (right,
default now). A node present at both points is `changed` when its versions or its
deployments differ. `from` after `to` answers `400 invalid-query`; a side whose scope has no
registered agent answers `404 scope-not-found`.

## Findings and blast radius

```bash
curl -s 'http://localhost:8085/api/v1/scopes/webshop/PROD/k8s-prod-eu1/findings?severity=HIGH' | jq .
curl -s 'http://localhost:8085/api/v1/scopes/webshop/PROD/k8s-prod-eu1/impact?node=db:postgresql/orders' | jq .
```

```json
[
  {
    "ruleId": "shared-database",
    "severity": "HIGH",
    "scope": { "project": "webshop", "environment": "PROD", "cluster": "k8s-prod-eu1" },
    "subjectNodeIds": ["db:postgresql/orders"],
    "title": "Database orders is shared by 2 services",
    "detail": "billing, orders use the database orders directly; a schema change for one of them affects all of them",
    "evidence": ["service:finance/billing", "service:sales/orders"],
    "evaluatedAt": "2026-10-05T12:00:00Z"
  }
]
```

The architecture rules run right after an ingested snapshot, at most once per scope per
`architrace.rules.evaluation-interval`, and replace the findings of the scope as a whole;
`evaluatedAt` is the point in time of the graph they ran on. A finding is about its
`subjectNodeIds` (a cycle lists every member, a chain every hop); `evidence` holds the node
ids that substantiate it in the order the rule chose: the shortest cycle, the clients of the
database, the callers, the impaired nodes, the chain. Names appear only in `title` and
`detail`. The list is ordered by severity (high first), rule id and subject ids; `severity`
and `rule` narrow it. The rule ids and their thresholds are on the
[configuration](../configuration/#control-plane) page.

```json
{
  "subject": { "id": "db:postgresql/orders", "type": "DATABASE", "name": "orders",
               "versions": [], "deployments": [], "labels": {} },
  "at": "2026-10-05T12:00:00Z",
  "impaired": [
    { "node": { "id": "service:sales/orders", "type": "SERVICE", "name": "orders", "versions": ["2.8.1"],
                "deployments": [{ "cluster": "k8s-prod-eu1", "namespace": "orders" }], "labels": {} },
      "distance": 1, "path": ["service:sales/orders", "db:postgresql/orders"] }
  ],
  "delayed": [
    { "node": { "id": "service:sales/reporting", "type": "SERVICE", "name": "reporting", "versions": ["1.2.0"],
                "deployments": [{ "cluster": "k8s-prod-eu1", "namespace": "reporting" }], "labels": {} },
      "distance": 3,
      "path": ["service:sales/reporting", "topic:kafka/order-events", "service:sales/orders", "db:postgresql/orders"] }
  ],
  "services": 1,
  "servicesTotal": 2
}
```

`impact` is computed on request from the current graph at `at`, like the drift, so it works
for any node and any point in time. `impaired` are the nodes that fail or error when the
subject fails: its callers over sync edges, transitively; for a topic its producers and
their callers. `delayed` are the nodes that keep running but receive their data late: the
consumers of the topics the subject or an impaired service publishes to, and the consumers
of the subject itself when it is a topic. Every entry carries its distance in hops and one
shortest path from the node to the subject; both lists are ordered by distance, then id, and
the subject is in neither. `services` counts the impaired service nodes, `servicesTotal` the
service nodes of the graph. A node id that is not in the graph answers `404 node-not-found`.

## Errors

Every error is an RFC 9457 problem with the media type `application/problem+json`. Errors
raised by the framework (unknown path, unreadable parameter) carry no `type`, which the RFC
reads as `about:blank`:

```json
{ "title": "Not Found", "status": 404, "instance": "/api/v1/nothing" }
```

Errors raised by the domain carry a stable type, a title and the reason:

| Type | Status | When |
|------|--------|------|
| `urn:architrace:problem:scope-not-found` | 404 | no agent has registered for the scope in the path, or for the left side of a diff |
| `urn:architrace:problem:snapshot-not-found` | 404 | no snapshot has the id, or retention removed it |
| `urn:architrace:problem:node-not-found` | 404 | the current graph of the scope has no node with the id given to `impact` |
| `urn:architrace:problem:invalid-query` | 400 | `page` or `size` out of range, `from` after `to` (history and timeline drift) |

```json
{
  "type": "urn:architrace:problem:scope-not-found",
  "title": "Scope not found",
  "status": 404,
  "detail": "no agent has registered for scope webshop/PROD/k8s-prod-eu2",
  "instance": "/api/v1/scopes/webshop/PROD/k8s-prod-eu2/graph"
}
```

## Stability

Until the first release the document may still change. Afterwards changes within `v1` are
additive; a breaking change creates `v2` next to it.
