---
title: M1. Agent pipeline completion
description: Standard OpenTelemetry in, complete metric-bearing graph snapshots out.
---

Status: in progress (PR 1 of 5 in review, ARCHI-38) · Order: 2 · Requirements: F1–F5, N1, N2, N6; defects A1–A12

## Goal

The agent turns traces from any standard OpenTelemetry instrumentation into complete graph
snapshots (services, databases, topics, external services, edges with metrics) and streams
them reliably to the control plane within bounded memory.

## Scope

In: span model, attribute mapping, node and edge building, metrics, windows and snapshots,
publishing, eviction, agent metrics endpoint, configuration v2, cleanup of dead and misplaced
code, tests without reflection.

Out: sampling, anomaly detection, remote configuration beyond acknowledging updates,
OTLP/HTTP receiver (post-MVP).

## Design

### Span model

`SpanRecord` replaces `InternalSpan`:

```
SpanRecord(traceId, spanId, parentSpanId, kind, startEpochNanos, endEpochNanos, error,
           ServiceIdentity(environment, domain, name, version),
           Deployment(cluster, namespace, instance),
           Peer peer)
sealed interface Peer permits Http(address, port), Database(system, namespace),
                              Messaging(system, destination, operation), None
```

Classification is a pattern-matching switch over `(kind, peer)`; no boolean flag methods.

### Attribute mapping

Each logical field has an ordered list of attribute keys and a default. Defaults cover current
and legacy semantic conventions; overrides live in the agent config.

| Field | Keys in priority order | Default |
|-------|------------------------|---------|
| environment | `deployment.environment.name`, `deployment.environment`, `environment` | agent config `environment` |
| domain | `service.namespace`, `domainId` | `default` |
| service | `service.name`, `serviceName` | required |
| version | `service.version` | `unknown` |
| cluster | `k8s.cluster.name`, `cluster` | agent config `clusterId` |
| namespace | `k8s.namespace.name`, `namespace` | |
| http peer | `server.address`, `net.peer.name`, `http.host` | |
| database | `db.system`, `db.namespace`, `db.name` | |
| messaging | `messaging.system`, `messaging.destination.name`, `messaging.destination`, `messaging.operation.type`, `messaging.operation` | |

### Nodes and edges

`sealed interface GraphNode permits ServiceNode, DatabaseNode, TopicNode, ExternalNode` with
stable ids ([ADR 0007](../../adr/0007-topology-model/)). Edge building by kind and peer:

| Span | Rule | Edge |
|------|------|------|
| SERVER with a CLIENT parent of another service | pair by `(traceId, parentSpanId)` in either arrival order | `source → target`, kind `sync` |
| CLIENT with `Database` peer | immediate | `service → db`, kind `sync` |
| CLIENT with `Http` peer | held until a SERVER child arrives or the TTL expires | matched: `sync` to the service; expired: `service → external` |
| PRODUCER with `Messaging` peer | immediate | `service → topic`, kind `publish` |
| CONSUMER with `Messaging` peer | immediate | `topic → service`, kind `consume` |
| INTERNAL | ignored | |

Edges carry `EdgeMetrics`: calls, errors, latency histogram with logarithmic buckets from
1 ms to 60 s, `p50`, `p95`, `p99` estimates, max. Metrics are per window and reset on
snapshot.

### Window, snapshot, publishing

- `GraphWindow` accumulates nodes (with versions and deployments seen) and edges for the
  current interval. Every `snapshot.interval` (default 60 s) the window is frozen into an
  immutable `GraphSnapshot`, converted to the `GraphBatch` protobuf and offered to a bounded
  outbound queue (default 64 snapshots, drop-oldest, drop counted).
- The control plane client drains the queue while a session is open; the queue survives
  reconnects. The start-up race in the current runtime disappears because the publisher
  writes to the queue, not to a session.
- The window owner is a single thread fed by the ring buffer; the snapshot scheduler swaps
  the window atomically.

### Eviction and bounds

- `PendingSpanIndex` holds spans waiting for a partner with a TTL (default 120 s), swept every
  10 s. Expired CLIENT spans with an HTTP peer become external edges; others are dropped.
- The ring buffer (default 65 536) rejects when full; rejections are counted and logged with
  rate limiting.

### Metrics and health

Micrometer registry with Prometheus exposition on `metrics.port` (default 9464) using the JDK
HTTP server: `/metrics`, `/health`. Metrics: `spans_received`, `spans_rejected`,
`spans_evicted`, `edges_active`, `snapshots_published`, `snapshots_dropped`,
`control_plane_connected`.

### Configuration v2

```yaml
project: webshop
environment: DEV
cluster: cluster-a
agent:
  name: cluster-a-agent
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

`project`, `environment` and `cluster` form the scope the agent registers with; `environment`
and `cluster` are also the fallbacks when a resource carries no environment or cluster
attribute. `dry-run` loads, validates and prints the effective configuration including mapping
defaults; `--prop key=value` overrides apply to both commands. The full key table is on the
[Configuration](../../../reference/configuration/) page.

### Cleanup

Delete from the agent: `ControlPlaneServiceImpl`, `ControlPlaneRegistry` (server code),
`GraphSnapshotService`, `NodeAggregator`, `SamplingProcessor`, `AnomalyDetectionProcessor`,
the duplicate `otlp.GraphNode`. Tests exercise public behaviour only; one integration test
drives OTLP in and asserts the `GraphBatch` out through in-process gRPC.

## Acceptance criteria

- The demo stack with standard OTel instrumentation yields service, database, topic and
  external nodes with sync, publish and consume edges and non-zero metrics.
- 10 000 spans/s sustained on 2 vCPU for 10 minutes with heap under 512 MB and no growth.
- Killing and restarting the control plane loses at most `queue-size` snapshots, all counted.
- Agent coverage ≥ 85 %; no reflection in tests; no dead classes.

## Delivery plan

1. `SpanRecord`, `Peer`, attribute mapping, normaliser, config v2 with `dry-run`.
2. Node and edge builder, pending index, metrics, window.
3. Snapshot scheduler, publisher of `GraphSnapshot` (the contract was extended in ARCHI-28; remove `GraphBatch`), queue, runtime fix.
4. Eviction sweeps, metrics endpoint, rate-limited logging, load test.
5. Cleanup, integration test, reference pages (configuration, CLI).

### PR 1: span model, attribute mapping, normaliser, configuration v2 (ARCHI-38)

What landed and the decisions behind it:

- **Span model** (`span` package): `SpanRecord` with `ServiceIdentity`, `Deployment` and the
  sealed `Peer` (`Http`, `Database`, `Messaging`, `None`). Components that may be absent
  (`parentSpanId`, namespace, instance, port, destination) are `Optional` instead of `null`, so
  pattern matching and record deconstruction never meet a null. Compact constructors reject
  missing required components. `latencyMillis()` is derived from the OTLP nanos and never
  negative.
- **Attribute mapping**: `MappedField` is an enum that carries the config key and the default
  key list of every field; `AttributeMapping` is an immutable record over an `EnumMap`, so the
  table is complete by construction and prints in declaration order. A configured override
  replaces the key list of one field entirely, which is easier to reason about than merging.
- **Normaliser** (`SpanNormaliser`): one pass per resource resolves identity and deployment,
  then every span of the resource gets its peer from the span attributes. Peer precedence is
  database, then messaging, then HTTP, because a database or messaging client span usually
  carries a server address as well. Resources without a service name are dropped: no identity,
  no node. Scalar attribute values of every type are read as text (ports arrive as integers);
  arrays and maps are ignored. Trace and span ids are lower-case hex, the error flag is the
  OTLP status code.
- **Configuration v2**: two records with different jobs. `AgentConfigDocument` is the Jackson
  face of the YAML, nullable and annotated with the kebab-case keys; `AgentConfig` is the
  effective configuration, non-null, with `Duration` instead of seconds. Validation runs on the
  document and reports every problem at once as an `AgentConfigException`, so `dry-run` shows
  the whole list instead of the first finding. Unknown keys are rejected by Jackson and
  reported with their path. `--prop` overrides are applied to the YAML tree before mapping:
  the key is a dotted path, the value is parsed as YAML, so numbers, booleans and lists work
  and overrides are validated exactly like file values. `dry-run` renders the effective
  configuration through the same document type, which is why the output loads back to an
  equal `AgentConfig` (tested). Both commands share a picocli `@Mixin` for `--config` and
  `--prop`.
- **Runtime**: the configuration now drives the ring size, the OTLP port, the retry delay and
  the snapshot interval (A7 in part). The snapshot loop looks the control plane session up on
  every tick instead of dereferencing it at start-up, which removes the start-up crash of A2;
  publishing itself stays a stub until PR 3. The legacy resolvers and registries consume
  `SpanRecord` through pattern matching on `Peer`; `InternalSpan`, `SpanExtractor`,
  `AttributeDictionary` and the empty sampling and anomaly processors are gone (A4, part of
  A5).
- **Tests without reflection**: commands are driven through `CommandLine.execute`, the
  runtime through its public `run` on a worker thread (OTLP port observed, failed session
  closed, interruption stops everything), the pipeline through records fed into
  `SpanPipeline`. The agent ratchet moves to line 0.85, branch 0.77, method 0.85. New and
  rewritten files are already in palantir format; the module-wide formatter switch stays off
  until the remaining legacy files are rewritten, because reformatting them would flood the
  SonarCloud new-code gate.
- **Working assumption**: `project` is an agent setting (default `default`), not a telemetry
  attribute; the scope decision pending since ARCHI-26 is resolved this way unless the
  maintainer objects.

## Risks and open points

- Pairing relies on `parentSpanId`; instrumentation that breaks context propagation produces
  external edges instead of service edges, which is the honest result.
- Topic naming across brokers (Kafka topic vs RabbitMQ exchange/queue) is normalised by
  `messaging.system` + destination; refinements are post-MVP.
