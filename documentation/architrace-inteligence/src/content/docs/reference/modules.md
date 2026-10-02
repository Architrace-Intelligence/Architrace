---
title: Modules
description: Monorepo module responsibilities.
---

## `architrace-agent`

- CLI entrypoint (`architrace`).
- OTLP trace receiver implementation.
- Span-to-graph conversion and control-plane publishing.

## `architrace-control-plane`

- Spring Boot application runtime.
- gRPC service implementation (`AgentService`).
- Handles agent stream and health responses.
- Serves the Query API, its OpenAPI document and Swagger UI, and the UI bundle at `/`.

## `architrace-api`

- Shared protobuf contract (`architrace-agent.proto`).
- OpenAPI 3.1 document of the Query API (`openapi/architrace-query-api.yaml`).
- Exposes gRPC/protobuf dependencies to consuming modules.

## `architrace-ui`

- Web UI: Vite, React, TypeScript, TanStack Query.
- Client typed from the OpenAPI document (`openapi-typescript`, `openapi-fetch`).
- Built by Gradle (`:ui:npmBuild`), tested by `:ui:test`; the bundle is a consumable Gradle
  configuration that the control plane packs under `static/`.

## `otel-test-app`

- End-to-end local demo with Python services.
- OpenTelemetry Collector forwarding traces to Architrace agent.
- Docker Compose orchestration for quick validation.
