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
| `GET /api/v1/agents` | every registered agent with its scope, version, first and last seen time and liveness, ordered by scope and name |

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

## Errors

Every error is an RFC 9457 problem with the media type `application/problem+json`:

```json
{ "type": "about:blank", "title": "Not Found", "status": 404, "instance": "/api/v1/nothing" }
```

## Stability

Until the first release the document may still change. Afterwards changes within `v1` are
additive; a breaking change creates `v2` next to it.
