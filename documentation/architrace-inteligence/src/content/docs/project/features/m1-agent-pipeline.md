---
title: M1. Agent pipeline completion
description: Standard OpenTelemetry in, complete metric-bearing graph snapshots out.
---

Status: design agreed · Order: 2 · Requirements: F1–F5, N1, N2, N6; defects A1–A12

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
environment: DEV
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

`dry-run` loads, validates and prints the effective configuration including mapping defaults;
`--prop key=value` overrides apply to both commands.

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

## Risks and open points

- Pairing relies on `parentSpanId`; instrumentation that breaks context propagation produces
  external edges instead of service edges, which is the honest result.
- Topic naming across brokers (Kafka topic vs RabbitMQ exchange/queue) is normalised by
  `messaging.system` + destination; refinements are post-MVP.
