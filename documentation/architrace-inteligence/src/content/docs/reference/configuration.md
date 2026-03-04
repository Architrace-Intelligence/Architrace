---
title: Configuration
description: Agent runtime configuration fields and examples.
---

## YAML schema

```yaml
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

- `clusterId`
- `agent.name`
- `control-plane.bootstrap.server`

## Optional fields

- `otlp-receiver-port` (must be `> 0` when provided)
- `control-plane-retry-seconds` (must be `> 0` when provided)

## Example (project demo)

```yaml
environment: DEV
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

`environment`, `domainId`, and `namespace` are present in demo config for topology context.
