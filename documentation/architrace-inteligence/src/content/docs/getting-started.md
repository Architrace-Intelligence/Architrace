---
title: Getting Started
description: Build and run Architrace locally.
---

## Prerequisites

- Java 25+
- Docker (optional, for the demo stack)
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

```bash
./gradlew :control-plane:bootRun
```

Default ports:

- HTTP: `8085`
- gRPC: `9090`

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
