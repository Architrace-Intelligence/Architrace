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

## `architrace-api`

- Shared protobuf contract (`architrace-agent.proto`).
- Exposes gRPC/protobuf dependencies to consuming modules.

## `otel-test-app`

- End-to-end local demo with Python services.
- OpenTelemetry Collector forwarding traces to Architrace agent.
- Docker Compose orchestration for quick validation.
