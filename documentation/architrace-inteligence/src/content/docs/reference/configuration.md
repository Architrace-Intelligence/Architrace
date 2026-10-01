---
title: Configuration
description: Agent and control plane configuration, defaults and examples.
---

## YAML schema

```yaml
environment: DEV
project: webshop
clusterId: cluster-1
agent:
  name: demo-agent
control-plane:
  bootstrap:
    server: localhost:9090
otlp-receiver-port: 4319
control-plane-retry-seconds: 5
```

## Required fields

- `environment`: the environment the agent observes (`DEV`, `STAGE`, `PROD`, …)
- `clusterId`: the Kubernetes cluster the agent runs in
- `agent.name`
- `control-plane.bootstrap.server`

`project`, `environment` and `clusterId` form the agent's scope. The control plane stores
every snapshot under that scope, and the UI lists one row per scope.

## Optional fields

- `project`: the project the agent reports under (default `default`)
- `otlp-receiver-port` (must be `> 0` when provided)
- `control-plane-retry-seconds` (must be `> 0` when provided)

## Example (project demo)

```yaml
environment: DEV
project: demo
clusterId: otel-test

domainId: demo
namespace: local

agent:
  name: docker-agent

control-plane:
  bootstrap:
    server: control-plane:9090

otlp-receiver-port: 4319
control-plane-retry-seconds: 5
```

`domainId` and `namespace` are ignored today; the agent pipeline (M1) replaces them with the standard OpenTelemetry resource attributes.

## Control plane

The control plane is a Spring Boot application; every property below can be set in
`application.yaml`, as a `--property=value` argument or as an environment variable.

| Property | Default | Meaning |
|----------|---------|---------|
| `ARCHITRACE_DB_URL`, `ARCHITRACE_DB_USERNAME`, `ARCHITRACE_DB_PASSWORD` | `jdbc:postgresql://localhost:5432/architrace`, `architrace`, `architrace` | PostgreSQL connection |
| `architrace.ingestion.snapshot-interval` | `60s` | snapshot interval sent to agents in `ConfigUpdate` |
| `architrace.ingestion.heartbeat-interval` | `30s` | heartbeat interval sent to agents; an agent is live while fewer than three intervals have passed since it was last seen |
| `architrace.topology.retention.period` | `30d` | snapshots whose window ended before `now - period` are deleted |
| `architrace.topology.retention.batch-size` | `1000` | snapshots deleted per statement |
| `architrace.topology.retention.cron` | `0 0 3 * * *` | when the retention job runs (UTC) |

Ports: HTTP `8085` (`server.port`), gRPC `9090` (`spring.grpc.server.port`). Actuator serves
`/actuator/health` and `/actuator/metrics`.
