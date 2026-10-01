---
title: M3. Query API
description: A versioned REST API, described by OpenAPI, that the UI and scripts use to read topology, snapshots, diffs and findings.
---

Status: design agreed · Order: 4 · Requirements: F7, N4

## Goal

Every view the UI needs is available as a documented HTTP endpoint that works equally well from
a script.

## Scope

In: OpenAPI document, generated server interfaces, controllers for environments, agents,
services, graph, snapshots; error model; Swagger UI; contract tests.

Out: diff endpoints (M5), findings endpoints (M6), authentication, write operations.

## Design

### Contract-first

The OpenAPI 3.1 document lives in `architrace-api/src/main/resources/openapi.yaml`
([ADR 0008](../../adr/0008-contract-first-apis/)). The Gradle build generates Spring interfaces
(`interfaceOnly`, Java records for models) that controllers implement; the same document feeds
the TypeScript types in M4. springdoc serves the document at `/api/v1/openapi.yaml` and
Swagger UI at `/swagger-ui`.

### Resources (`/api/v1`)

| Method and path | Returns |
|-----------------|---------|
| `GET /environments` | environments with agent counts and last snapshot time |
| `GET /environments/{env}/graph?at=` | current `TopologyGraph` (nodes, edges with metrics) |
| `GET /environments/{env}/services` | services with versions, deployments, degree |
| `GET /environments/{env}/services/{serviceId}` | service detail with inbound and outbound edges |
| `GET /environments/{env}/snapshots?from=&to=&page=` | snapshot list, paginated |
| `GET /snapshots/{id}` | one snapshot with its graph |
| `GET /agents` | registered agents with liveness |
| `GET /diff/…`, `GET /environments/{env}/findings` | reserved for M5 and M6 |

Conventions: `at`, `from`, `to` are RFC 3339 timestamps; ids are the stable node ids; lists
are paginated with `page` and `size` and return `Page<T>`; errors are RFC 9457
`application/problem+json` with a stable `type` URI per error.

### Implementation

- Controllers are thin: validate nothing the generated layer already validated, call one
  service, map to the generated model.
- A `ProblemDetailsAdvice` converts domain exceptions (`EnvironmentNotFound`, …) to problems.
- Contract tests with MockMvc per endpoint plus an OpenAPI validation step in the build
  (document must be valid, generated code must compile).

## Acceptance criteria

- Swagger UI shows every endpoint with examples; every endpoint has a contract test.
- A script using only `curl` can list environments and download a graph as JSON.
- Breaking changes to the document fail the build when the TypeScript client (M4) is regenerated.

## Delivery plan

1. OpenAPI document, generator wiring, environments and agents endpoints, problem details.
2. Graph, services and snapshots endpoints with pagination.
3. API reference page on the site, examples, architecture page update.

## Risks and open points

- Generator support for OpenAPI 3.1 and Java records is verified in PR 1; fallback is 3.0.3.
