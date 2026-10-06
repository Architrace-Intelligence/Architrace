---
title: Deploying Architrace
description: Run the control plane and one agent per environment in your own infrastructure, and instrument Spring Boot services so the agent sees every dependency.
---

Architrace is two images and a database. The control plane stores snapshots and serves the
UI and the Query API; one agent per environment receives OTLP traces, usually from an
OpenTelemetry Collector, and streams the service graph to the control plane. Images:
`ghcr.io/architrace-intelligence/architrace-control-plane` and
`ghcr.io/architrace-intelligence/architrace-agent`, tags `X.Y.Z`, `latest` and `sha-<short>`,
non-root, with a `HEALTHCHECK` each.

## Control plane

| Setting | Default | Meaning |
|---------|---------|---------|
| `ARCHITRACE_DB_URL`, `ARCHITRACE_DB_USERNAME`, `ARCHITRACE_DB_PASSWORD` | `jdbc:postgresql://localhost:5432/architrace`, `architrace`, `architrace` | PostgreSQL 15 or newer; Liquibase applies the schema on start-up |
| `8085` | HTTP | UI, Query API (`/api/v1`), Swagger UI, Actuator (`/actuator/health`, `/actuator/metrics`) |
| `9090` | gRPC | the agents' bidirectional stream |

Every property of the [configuration](../../reference/configuration/#control-plane) page can be
set as an environment variable in Spring's relaxed form (`architrace.ingestion.snapshot-interval`
is `ARCHITRACE_INGESTION_SNAPSHOTINTERVAL`). Snapshots are retained for thirty days by default;
the database grows with the number of agents, their interval and the size of the graph, a few
megabytes per agent and day for a graph of a hundred services. Put the UI behind your usual
reverse proxy and authentication: the control plane has no user accounts in the MVP.

## Agents

Run **one agent per environment** (and per cluster if the clusters are separate scopes): an
agent registers with its project, environment and cluster and drops spans whose
`deployment.environment.name` is another environment. The agent reads
`/config/architrace-agent.yaml` (mount it), listens for OTLP/gRPC on `otlp.port` (default
`4319`) and serves `/metrics` and `/health` on `metrics.port` (default `9464`, which the image
health check probes; set `ARCHITRACE_METRICS_PORT` when you change it). It needs the control
plane's gRPC address and nothing else; it keeps a bounded queue of snapshots while the control
plane is unreachable and reconnects on its own.

```yaml
project: webshop
environment: PROD
cluster: eu-1
agent:
  name: eu-1-prod
control-plane:
  server: control-plane.architrace.svc:9090
otlp:
  port: 4319
```

Point the collector of that environment at the agent:

```yaml
exporters:
  otlp/architrace:
    endpoint: architrace-agent.observability.svc:4319
    tls:
      insecure: true
service:
  pipelines:
    traces/architrace:
      receivers: [otlp]
      exporters: [otlp/architrace]
```

One collector serving several environments routes by resource attribute instead; the
[demo](../docker-demo/) does exactly that with the routing connector. Sizing: the agent holds
the current window in memory (nodes, edges, pending spans); a few hundred megabytes of heap
cover thousands of spans per second, and `buffers.ring-size` bounds the queue in front of the
graph worker. Every key is on the [configuration](../../reference/configuration/#agent) page.

## Instrumenting Spring Boot services

The agent only sees what the traces say, so the instrumentation decides which dependencies
appear on the map. Two options exist for Spring Boot:

**The OpenTelemetry Spring Boot starter** (`opentelemetry-spring-boot-starter`) configured
through the usual environment variables:

```text
OTEL_SERVICE_NAME=orders
OTEL_RESOURCE_ATTRIBUTES=service.namespace=sales,service.version=2.8.1,deployment.environment.name=PROD,k8s.cluster.name=eu-1,k8s.namespace.name=sales
OTEL_EXPORTER_OTLP_PROTOCOL=grpc
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_TRACES_EXPORTER=otlp
OTEL_METRICS_EXPORTER=none
OTEL_LOGS_EXPORTER=none
```

`OTEL_EXPORTER_OTLP_PROTOCOL=grpc` matters: the SDKs default to HTTP/protobuf, which a gRPC
port (`4317` on a collector, `4319` on the agent) does not accept.

The starter covers Spring MVC and WebFlux servers, `RestTemplate`, `WebClient` and
`RestClient` calls, JDBC and R2DBC, and Spring Kafka listeners and templates. That is enough
for a service that talks HTTP, SQL and Spring Kafka.

**The OpenTelemetry Java agent** (`-javaagent:opentelemetry-javaagent.jar`, same variables) is
needed when the service uses clients the starter does not instrument: Kafka Streams,
reactor-kafka, raw `KafkaProducer` and `KafkaConsumer`, Lettuce and other Redis clients, OkHttp
and most other HTTP clients. Without it those dependencies stay invisible on the map. The
Kafka interceptors shipped with the starter do not help here: the starter registers no global
OpenTelemetry instance, so the interceptors record nothing.

Either way the Prometheus metrics of the service stay untouched: Architrace reads traces only,
and `OTEL_METRICS_EXPORTER=none` keeps the OpenTelemetry SDK from exporting a second set.

What the agent needs from a span: `service.name` and `service.namespace` for the service and
its domain, `service.version`, `deployment.environment.name`, `k8s.cluster.name` and
`k8s.namespace.name` for the scope and the deployment, `server.address` on client spans for
the peer, `db.system` and `db.namespace` for databases, `messaging.system` and
`messaging.destination.name` for topics. The [configuration](../../reference/configuration/#attribute-mapping)
page lists the attribute keys the agent tries for each field and how to add your own.

## Checklist

1. PostgreSQL reachable from the control plane; `ARCHITRACE_DB_*` set.
2. Control plane up: `/actuator/health` answers `UP`, the UI opens on `8085`.
3. One agent per environment with its YAML mounted; `/health` on `9464` answers
   `{"status":"UP","controlPlane":"CONNECTED"}`.
4. The collector of each environment exports OTLP/gRPC to its agent on `4319`.
5. Services carry the resource attributes above; the Projects list shows the scope within one
   snapshot interval (60 s by default) and the map fills with the first traffic.
