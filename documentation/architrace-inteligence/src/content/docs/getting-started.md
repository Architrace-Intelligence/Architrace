---
title: Getting Started
description: Build and run Architrace locally.
---

## Prerequisites

- Java 25+
- Docker (for the PostgreSQL database, the integration tests and the demo stack)
- Node.js (for this docs site only)

## Repository structure

- `architrace-agent`: runtime agent CLI and OTLP ingestion.
- `architrace-control-plane`: Spring Boot control-plane (HTTP + gRPC).
- `architrace-api`: shared protobuf contract and generated types.
- `otel-test-app`: end-to-end demo services and OpenTelemetry collector.

## Build all modules

```bash
./gradlew spotlessCheck classes test jacocoTestReport
./gradlew build
```

## Run control-plane locally

The control plane needs PostgreSQL. The demo stack ships one; start it first:

```bash
docker compose -f otel-test-app/docker-compose.yml up -d postgres
./gradlew :control-plane:bootRun
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

The integration tests of the control plane start their own PostgreSQL through Testcontainers, so
`./gradlew test` needs a running Docker daemon.

## Build and run agent

Build fat jar:

```bash
./gradlew :agent:shadowJar
```

Run with config:

```bash
java -jar architrace-agent/build/libs/agent-0.1.0-all.jar run --config ./otel-test-app/architrace-agent.yaml
```

Agent OTLP receiver listens on `otlp-receiver-port` from config (demo uses `4319`).

## Docs development

```bash
cd documentation/architrace-inteligence
npm install
npm run dev
```
