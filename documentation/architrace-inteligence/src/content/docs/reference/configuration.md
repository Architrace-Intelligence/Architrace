---
title: Configuration
description: Agent runtime configuration fields and examples.
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
