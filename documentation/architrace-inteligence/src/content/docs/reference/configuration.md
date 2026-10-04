---
title: Configuration
description: Agent and control plane configuration, defaults and examples.
---

## Agent

The agent reads one YAML file (`run --config <path>`, `dry-run --config <path>`). Every value
can be overridden on the command line with `--prop <dotted.key>=<value>`; the value is parsed
as YAML, so numbers, booleans and lists (`[a, b]`) work. `dry-run` prints the effective
configuration after defaults and overrides, or lists every problem when the file is invalid.

```yaml
project: webshop
environment: PROD
cluster: eu-1
agent:
  name: eu-1-agent
control-plane:
  server: control-plane:9090
  retry-seconds: 5
otlp:
  port: 4319
snapshot:
  interval-seconds: 60
  queue-size: 64
buffers:
  ring-size: 65536
  pending-ttl-seconds: 120
metrics:
  port: 9464
attribute-mapping:
  domain: [service.namespace, team]
```

| Key | Required | Default | Meaning |
|-----|----------|---------|---------|
| `project` | no | `default` | project the agent reports under; with `environment` and `cluster` it forms the agent's scope |
| `environment` | yes | | environment used when a resource carries no `deployment.environment.name` |
| `cluster` | yes | | cluster used when a resource carries no `k8s.cluster.name` |
| `agent.name` | yes | | agent identity at registration |
| `control-plane.server` | yes | | `host:port` of the control plane gRPC endpoint |
| `control-plane.retry-seconds` | no | `5` | pause before a new control plane session after a failure |
| `otlp.port` | no | `4319` | OTLP/gRPC receiver port (`1`–`65535`) |
| `snapshot.interval-seconds` | no | `60` | window length; one snapshot per interval |
| `snapshot.queue-size` | no | `64` | snapshots kept while the control plane is unreachable (oldest dropped) |
| `buffers.ring-size` | no | `65536` | span ring buffer between the receiver and the pipeline; a power of two |
| `buffers.pending-ttl-seconds` | no | `120` | how long a span waits for its partner before eviction |
| `metrics.port` | no | `9464` | Prometheus metrics and health endpoint port |
| `attribute-mapping.<field>` | no | see below | attribute keys tried in order for one field; replaces the default list of that field |

Unknown keys are rejected, so a typo cannot silently disable a setting. The control plane
stores every snapshot under the scope `project` × `environment` × `cluster`, and the UI lists
one row per scope.

### Attribute mapping

Service identity, deployment and peers are read from OpenTelemetry attributes. Each field has an
ordered list of keys; the first key present with a non-blank value wins. The defaults cover the
current semantic conventions first and legacy names after them.

| Field | Keys in priority order | Fallback |
|-------|------------------------|----------|
| `environment` | `deployment.environment.name`, `deployment.environment`, `environment` | config `environment` |
| `domain` | `service.namespace`, `domainId` | `default` |
| `service` | `service.name`, `serviceName` | spans without it are dropped |
| `version` | `service.version` | `unknown` |
| `cluster` | `k8s.cluster.name`, `cluster` | config `cluster` |
| `namespace` | `k8s.namespace.name`, `namespace` | none |
| `instance` | `service.instance.id`, `k8s.pod.name` | none |
| `http-address` | `server.address`, `net.peer.name`, `http.host` | no HTTP peer |
| `http-port` | `server.port`, `net.peer.port` | unknown port |
| `db-system` | `db.system` | no database peer |
| `db-namespace` | `db.namespace`, `db.name` | none |
| `messaging-system` | `messaging.system` | no messaging peer |
| `messaging-destination` | `messaging.destination.name`, `messaging.destination` | none |
| `messaging-operation` | `messaging.operation.type`, `messaging.operation` | none |

The first seven fields are resource attributes, the rest span attributes. A span is classified
as a database call when `db-system` resolves, else as messaging when `messaging-system`
resolves, else as HTTP when `http-address` resolves.

### Example (demo stack)

```yaml
project: demo
environment: DEV
cluster: otel-test

agent:
  name: docker-agent

control-plane:
  server: control-plane:9090
  retry-seconds: 5

otlp:
  port: 4319
```

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
