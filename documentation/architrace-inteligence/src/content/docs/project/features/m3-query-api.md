---
title: M3. Query API
description: A versioned REST API, described by OpenAPI, that the UI and scripts use to read topology, snapshots, diffs and findings.
---

Status: in progress (PR 1 of 3 delivered: document, generator, scopes and agents endpoints, problem details) · Order: 4 · Requirements: F7, N4

## Goal

Every view the UI needs is available as a documented HTTP endpoint that works equally well from
a script.

## Scope

In: OpenAPI document, generated server interfaces, controllers for environments, agents,
services, graph, snapshots; error model; Swagger UI; contract tests.

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
environment alone.

| Method and path | Returns | Status |
|-----------------|---------|--------|
| `GET /scopes` | one `ScopeSummary` per scope with a registered agent: agents, live agents, services, data streams, namespaces, end of the newest snapshot window | delivered |
| `GET /agents` | registered agents with scope, versions, first and last seen, liveness | delivered |
| `GET /scopes/{project}/{environment}/{cluster}/graph?at=` | current `TopologyGraph` (nodes, edges with metrics) | PR 2 |
| `GET /scopes/{project}/{environment}/{cluster}/services` | services with versions, deployments, degree | PR 2 |
| `GET /scopes/{project}/{environment}/{cluster}/services/{serviceId}` | service detail with inbound and outbound edges | PR 2 |
| `GET /scopes/{project}/{environment}/{cluster}/snapshots?from=&to=&page=` | snapshot list, paginated | PR 2 |
| `GET /snapshots/{id}` | one snapshot with its graph | PR 2 |
| `GET /diff/…`, `…/findings` | reserved for M5 and M6 | |

Conventions: `at`, `from`, `to` are RFC 3339 timestamps; ids are the stable node ids; the
scope and agent lists are bounded and not paginated, snapshot lists are paginated with `page`
and `size`; errors are RFC 9457 `application/problem+json`.

### Implementation

- Controllers implement the generated interfaces and stay thin: one call into
  `TopologyQuery`, one mapping to the generated model (`ApiModels`). Base path `/api/v1`
  comes from a class-level `@RequestMapping` because the generator puts no base path on
  interfaces.
- Problem details: `spring.mvc.problemdetails.enabled` turns every framework error (unknown
  path, unreadable parameter) into `application/problem+json`. A `ProblemDetailsAdvice` for
  domain exceptions (`ScopeNotFound`, …) arrives with the first such exception in PR 2.
- Tests: `QueryApiTest` is a `@WebMvcTest` over the in-memory stores and a fixed clock,
  asserting the JSON with `MockMvcTester`; `OpenApiDocumentControllerTest` serves the
  document; `ControlPlaneApplicationTests` checks Swagger UI and the absence of a runtime
  document.

### Code shape (PR 1)

- `web`: `ApiPaths` (base path, document and Swagger UI paths), `WebConfiguration`
  (`/swagger-ui` → `/swagger-ui/index.html`), `OpenApiDocumentController`, the static page
  `static/swagger-ui/index.html`.
- `topology.web`: `ScopesController`, `AgentsController`, `ApiModels`.
- `topology`: `TopologyQuery.agents()` returns `AgentStatus` (agent plus liveness) ordered by
  scope and name.
- Build: `openapi-generator` plugin (`openApiGenerate` before `compileJava`, `openApiValidate`
  in `check`), `swagger-ui` and `webjars-locator-lite` dependencies; the generated package is
  excluded from JaCoCo and Sonar coverage, `build/generated` from Sonar analysis.

## Acceptance criteria

- Swagger UI shows every endpoint with examples; every endpoint has a contract test.
- A script using only `curl` can list environments and download a graph as JSON.
- Breaking changes to the document fail the build when the TypeScript client (M4) is regenerated.

## Delivery plan

1. OpenAPI document, generator wiring, scopes and agents endpoints, problem details (ARCHI-30,
   delivered).
2. Graph, services and snapshots endpoints with pagination.
3. API reference page on the site, examples, architecture page update.

## Risks and open points

- Generator support for OpenAPI 3.1 is verified (beta, works); Java records are not supported by the
  `spring` generator, models are generated classes (see Contract-first).
