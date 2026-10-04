---
title: Getting Started
description: Build and run Architrace locally.
---

## Prerequisites

- Java 25+
- Docker (for the PostgreSQL database, the integration tests and the demo stack)
- Node.js 22.12+ only for the UI dev loop and this docs site; the Gradle build downloads its
  own pinned Node.js for building and testing the UI

## Repository structure

- `architrace-agent`: runtime agent CLI and OTLP ingestion.
- `architrace-control-plane`: Spring Boot control-plane (HTTP + gRPC).
- `architrace-api`: shared protobuf and OpenAPI contracts and generated types.
- `architrace-ui`: web UI (Vite, React, TypeScript), bundled into the control plane jar.
- `otel-test-app`: end-to-end demo services and OpenTelemetry collector.

## Build all modules

```bash
./gradlew spotlessApply check
./gradlew build
```

`check` runs Spotless, Checkstyle, the tests with the JaCoCo coverage gate, the UI gate
(`:architrace-ui:test`: types, lint, formatting, unit tests) and the tests of the build logic.
The first build downloads Node.js and the UI packages; `build` packs the UI bundle into the
control plane jar. The version of every artifact comes from git (`./gradlew printVersion`):
`X.Y.Z` on a release tag, `X.Y.Z-<sha>-SNAPSHOT` anywhere else.

## Run control-plane locally

The control plane needs PostgreSQL. The demo stack ships one; start it first:

```bash
docker compose -f otel-test-app/docker-compose.yml up -d postgres
./gradlew :architrace-control-plane:bootRun
```

Liquibase applies the schema on start-up. Connection settings default to
`jdbc:postgresql://localhost:5432/architrace` with user and password `architrace`; override them
with `ARCHITRACE_DB_URL`, `ARCHITRACE_DB_USERNAME` and `ARCHITRACE_DB_PASSWORD`.

Default ports:

- HTTP: `8085`
- gRPC: `9090`
- PostgreSQL: `5432`

Health and metrics are on Actuator: `http://localhost:8085/actuator/health` and
`http://localhost:8085/actuator/metrics`.

The [Query API](../reference/query-api/) is under `http://localhost:8085/api/v1`: `/scopes`,
`/scopes/{project}/{environment}/{cluster}/graph`, `…/services`, `…/snapshots`,
`/snapshots/{id}` and `/agents`. Its OpenAPI document is at `/api/v1/openapi.yaml` and Swagger
UI at `/swagger-ui`. The web UI is at `http://localhost:8085/`.

The integration tests of the control plane start their own PostgreSQL through Testcontainers, so
`./gradlew test` needs a running Docker daemon.

## Build and run agent

Build fat jar:

```bash
./gradlew :architrace-agent:shadowJar
```

Run with config (the jar name carries the computed version):

```bash
java --enable-preview -jar architrace-agent/build/libs/architrace-agent-*-all.jar run --config ./otel-test-app/architrace-agent.yaml
```

Agent OTLP receiver listens on `otlp-receiver-port` from config (demo uses `4319`).

## UI development

The UI dev server rebuilds on every change and proxies `/api` to a control plane on port
`8085`, so start the control plane first:

```bash
cd architrace-ui
npm ci
npm run dev
```

`npm run check` runs the same gate as `./gradlew :architrace-ui:test`; `npm run generate` refreshes the
TypeScript types from the OpenAPI document (it runs automatically before `dev`, `build` and
`check`). Node.js 22.12+ is required; the Gradle build keeps its own copy under `.gradle/nodejs/` at
the repository root.

## Docs development

```bash
cd documentation/architrace-inteligence
npm install
npm run dev
```

## Next steps

- [Local Development](../guides/local-development/): run the modules from source, tests,
  dependency changes, versions and release notes.
- [Docker Demo](../guides/docker-demo/): the demo stack with instrumented services and a collector.
- [Contributing](../project/contributing/): how a change reaches `main`, the checks it has to
  pass and the commit convention.
