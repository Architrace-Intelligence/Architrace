---
title: Modules
description: Monorepo module responsibilities.
---

## `build-logic`

- Gradle included build with the convention plugins every module applies.
- `architrace.java`: JDK 25 toolchain with preview features, Spotless (palantir-java-format and
  the SPDX header), Checkstyle, JaCoCo with the coverage gate, JUnit platform, manifest
  `Implementation-Version`.
- `architrace.spring-boot`: Spring Boot and dependency management on top of `architrace.java`,
  build info, no plain jar.
- `architrace.versioning`: version from git tags and Conventional Commits through axion-release;
  tasks `printVersion`, `printReleaseVersion`, `releaseNotes`.

## `architrace-agent`

- CLI entrypoint (`architrace`): `run`, `dry-run`, `version`; configuration v2 (`core.config`).
- OTLP trace receiver (`otlp`) and span normalisation (`span`): `SpanRecord`, attribute mapping.
- Graph building (`graph`, `pipeline`): nodes and edges, pending index, window, snapshot; the
  worker thread and the bounded span queue.
- Control plane client (`controlplane`, `grpc`, `publish`): registration, session, snapshot
  queue and publishing.
- Metrics (`metrics`): Micrometer binding, Prometheus and health endpoint, drop reporting.

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

- Web UI: Vite, React, TypeScript, TanStack Query, React Flow with ELK for the service map.
- Client typed from the OpenAPI document (`openapi-typescript`, `openapi-fetch`).
- Built by Gradle (`:architrace-ui:npmBuild`), tested by `:architrace-ui:test`; the bundle is a consumable Gradle
  configuration that the control plane packs under `static/`.

## `otel-test-app`

- End-to-end local demo with Python services.
- OpenTelemetry Collector forwarding traces to Architrace agent.
- Docker Compose orchestration for quick validation.
