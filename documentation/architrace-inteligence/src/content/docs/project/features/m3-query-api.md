---
title: M3. Query API
description: A versioned REST API, described by OpenAPI, that the UI and scripts use to read topology, snapshots, diffs and findings.
---

Status: delivered (PR 1: document, generator, scopes and agents; PR 2: graph, services, snapshot
history, typed problem details, reference and architecture pages) · Order: 4 · Requirements: F7, N4

## Goal

Every view the UI needs is available as a documented HTTP endpoint that works equally well from
a script.

## Scope

In: OpenAPI document, generated server interfaces, controllers for scopes, agents, graph,
services and snapshots; error model; Swagger UI; contract tests.

Out: diff endpoints (M5), findings endpoints (M6), authentication, write operations.

## Design

### Contract-first

The OpenAPI 3.1 document lives in
`architrace-api/src/main/resources/openapi/architrace-query-api.yaml`
([ADR 0008](../../adr/0008-contract-first-apis/)). The control plane build generates Spring
server interfaces and models from it with openapi-generator (`spring`, `interfaceOnly`, one
interface per tag, models suffixed `Dto`) into `build/generated/openapi`; generated code is
never committed and `openApiValidate` runs with `check`. The same document feeds the
TypeScript types in M4. The control plane serves the document at `/api/v1/openapi.yaml` and
Swagger UI (the `swagger-ui` webjar with a static page that loads that document) at
`/swagger-ui`. There is no runtime-generated document: the contract file is the only one.

Verified in PR 1: openapi-generator 7.14.0 reads the 3.1 document (its 3.1 support is marked
beta, a warning only). The `spring` generator has no option for Java records, so models are
generated classes with required-args constructors and fluent setters; the `Dto` suffix keeps
them apart from the domain records they are mapped from. The generator plugin is declared in
the root build next to the other plugins so that its Jackson version is resolved together with
theirs.

### Resources (`/api/v1`)

Since ARCHI-26 the unit of the API is the scope (project, environment, cluster), not the
environment alone. Tags, and therefore generated interfaces: `scopes` (list, graph, services),
`snapshots` (history, one snapshot), `agents`.

| Method and path | Returns | Status |
|-----------------|---------|--------|
| `GET /scopes` | one `ScopeSummary` per scope with a registered agent: agents, live agents, services, data streams, namespaces, end of the newest snapshot window | delivered |
| `GET /agents` | registered agents with scope, versions, first and last seen, liveness | delivered |
| `GET /scopes/{project}/{environment}/{cluster}/graph?at=` | the current `TopologyGraph` at `at` (default now): nodes with versions, deployments and labels, edges with metrics | delivered |
| `GET /scopes/{project}/{environment}/{cluster}/services?at=` | one `NodeView` per service node of that graph: the node, its inbound edges (callers, consumed streams) and outbound edges (callees, databases, externals, published streams), each with the node at the other end | delivered |
| `GET /scopes/{project}/{environment}/{cluster}/snapshots?from=&to=&page=&size=` | `SnapshotPage`: summaries (id, agent, window, received, node and edge counts) newest first, window end within `[from, to]` | delivered |
| `GET /snapshots/{snapshotId}` | one `Snapshot` as reported: nodes and edges before any merging | delivered |
| `GET /diff/…`, `…/findings` | reserved for M5 and M6 | |

Conventions: `at`, `from`, `to` are RFC 3339 timestamps; ids are the stable node ids; the
scope, agent and service lists are bounded by the graph and not paginated; the snapshot
history is paginated with `page` (zero-based) and `size` (1 to 200, default 50); errors are
RFC 9457 `application/problem+json`.

A per-service resource (`…/services/{serviceId}`) was planned and dropped. Node ids contain
`/` (`db:postgresql/orders`), which servlet containers reject inside a path segment unless
encoded slashes are switched on, and a second key would duplicate the node id. The service
list therefore embeds the full dependency view of every service; the UI selects a service from
the graph it already holds and scripts filter the list. An `id` query filter can be added if
the list ever grows too large.

### Errors

Framework errors (unknown path, unreadable parameter) are rendered by Spring Boot's
problem-details support without a `type`, which RFC 9457 reads as `about:blank`. Domain errors
are mapped by `ProblemDetailsAdvice` in `topology.web` to a stable type:

| Type | Status | Raised by |
|------|--------|-----------|
| `urn:architrace:problem:scope-not-found` | 404 | graph, services or history of a scope without a registered agent |
| `urn:architrace:problem:snapshot-not-found` | 404 | `GET /snapshots/{snapshotId}` |
| `urn:architrace:problem:invalid-query` | 400 | `page` or `size` out of range, `from` after `to` |

The URN form is stable, claims no hostname and needs no page behind it; `title` names the
type, `detail` carries the reason, `instance` the request path.

### Implementation

- Controllers implement the generated interfaces and stay thin: one call into
  `TopologyQuery`, one mapping to the generated model (`ApiModels`). Base path `/api/v1`
  comes from a class-level `@RequestMapping` because the generator puts no base path on
  interfaces. `ScopesController` resolves a missing `at` to the clock's now.
- `TopologyQuery` requires a registered agent before answering the graph, services or history
  of a scope (`ScopeNotFoundException`), derives services from the current graph through
  `NodeViews` (a pure function: edges grouped by target and by source, joined with the node at
  the other end), pages the history through `SnapshotStore.list` and resolves one snapshot by
  id (`SnapshotNotFoundException`).
- Query parameters are validated by the domain records that carry them: `PageRequest`
  (page ≥ 0, 1 ≤ size ≤ 200) and `SnapshotFilter` (from ≤ to) throw `InvalidQueryException`;
  unparsable timestamps never reach them (framework 400).
- Pagination: the `snapshot` table carries `node_count` and `edge_count`, so the history is a
  projection query (`SnapshotSummaryRow`, no node or edge rows loaded) plus a count query,
  both filtered on `window_end` with optional bounds written as
  `cast(:from as timestamptz) is null or window_end >= :from`, so that a null bound is typed
  for PostgreSQL. The domain `Page<T>` holds items, the request and the total; `totalPages`
  is derived.
- Tests: `QueryApiTest` (`@WebMvcTest` over the in-memory stores and a fixed clock) asserts
  the JSON of every endpoint and every problem type with `MockMvcTester`; `TopologyQueryTest`
  and `NodeViewsTest` cover the domain; `JdbcSnapshotStoreTest` covers the paged projection on
  PostgreSQL; `OpenApiDocumentControllerTest` serves the document.

### Code shape

- `web`: `ApiPaths`, `WebConfiguration` (`/swagger-ui` → `/swagger-ui/index.html`),
  `OpenApiDocumentController`, the static page `static/swagger-ui/index.html`.
- `topology.web`: `ScopesController` (scopes, graph, services), `SnapshotsController`
  (history, one snapshot), `AgentsController`, `ApiModels`, `ProblemDetailsAdvice`.
- `topology`: `NodeView`, `Dependency`, `NodeViews`; `SnapshotSummary`, `SnapshotFilter`,
  `PageRequest`, `Page<T>`; `ScopeNotFoundException`, `SnapshotNotFoundException`,
  `InvalidQueryException`; `SnapshotStore.list`; `TopologyQuery.services`, `snapshots`,
  `snapshot`; `TopologyQuery.agents()` returns `AgentStatus` ordered by scope and name.
- `topology.persistence`: `SnapshotSummaryRow` projection, `SnapshotRepository.findSummaries`
  and `countSummaries`.
- Build: `openapi-generator` plugin (`openApiGenerate` before `compileJava`, `openApiValidate`
  in `check`), `swagger-ui` and `webjars-locator-lite` dependencies; the generated package is
  excluded from JaCoCo and Sonar coverage, `build/generated` from Sonar analysis.

## Acceptance criteria

- Swagger UI shows every endpoint with examples; every endpoint has a contract test. Met.
- A script using only `curl` can list scopes and download a graph as JSON. Met.
- Breaking changes to the document fail the build when the TypeScript client (M4) is
  regenerated. Verified with M4.

## Delivery plan

1. OpenAPI document, generator wiring, scopes and agents endpoints, problem details (ARCHI-30,
   delivered).
2. Graph, services and snapshot history endpoints with pagination, typed problem details
   (ARCHI-31, delivered).
3. API reference page with examples, architecture page update: folded into PR 2.

## Risks and open points

- Generator support for OpenAPI 3.1 is verified (beta, works); Java records are not supported
  by the `spring` generator, models are generated classes (see Contract-first).
- Node ids in paths: see Resources. Should a per-node resource become necessary, the options
  are an `id` query parameter or enabling encoded slashes in Tomcat with URL-encoded ids.
- The history filter applies to the window end only: a snapshot whose window starts before
  `from` but ends inside the range is included. Overlap semantics can be added without
  breaking the contract if the timeline view needs them.
