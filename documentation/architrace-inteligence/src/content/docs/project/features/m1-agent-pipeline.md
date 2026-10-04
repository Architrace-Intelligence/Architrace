---
title: M1. Agent pipeline completion
description: Standard OpenTelemetry in, complete metric-bearing graph snapshots out.
---

Status: done (PR 1–5 merged as #50–#54; the demo-stack check is part of M7) · Order: 2 · Requirements: F1–F5, N1, N2, N6; defects A1–A12

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
- The window owner is a single thread fed by the span queue; a freeze request is handed to
  that thread and answered with the frozen snapshot, so no lock guards the window.

### Eviction and bounds

- `PendingSpanIndex` holds spans waiting for a partner with a TTL (default 120 s), swept every
  10 s. Expired CLIENT spans with an HTTP peer become external edges; others are dropped.
- The span queue (default 65 536) rejects when full; rejections are counted and logged with
  rate limiting.

### Metrics and health

Micrometer registry with Prometheus exposition on `metrics.port` (default 9464) using the JDK
HTTP server: `/metrics`, `/health`. The metric names are listed on the
[Configuration](../../../reference/configuration/#metrics-and-health) page.

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

### PR 2: graph model, edge builder, pending index, window (ARCHI-39)

What landed and the decisions behind it:

- **Graph model** (`graph` package): sealed `GraphNode` with `ServiceNode`, `DatabaseNode`,
  `TopicNode` and `ExternalNode`; every record computes its id from its components exactly as
  [ADR 0007](../../adr/0007-topology-model/) specifies (`service:{domain}/{name}`,
  `db:{system}/{namespace}` with the namespace omitted when absent, `topic:{system}/{name}`,
  `ext:{address}`), so an id can never disagree with the identity it is derived from.
  `EdgeKey(source, target, kind)`, `EdgeObservation` (one call seen on an edge),
  `EdgeMetricsSummary` and the immutable `GraphSnapshot` with `SnapshotNode` (versions,
  deployments as `Placement(cluster, namespace)`) and `SnapshotEdge`, which mirror the protobuf
  messages one to one for PR 3.
- **Edge builder**: a switch over the span kind, then over the sealed peer. Database clients,
  producers and consumers produce their edge immediately; HTTP and peer-less clients and
  servers with a parent go through the `PendingSpanIndex`, which holds clients by their own
  span id and servers by their parent id, so the pair forms in either arrival order. Edge
  metrics are the client's view of a call (its latency and error flag). A match within one
  service, a root server, an internal span and a messaging span without a destination produce
  nothing. Expiry turns HTTP clients into `service → external` edges and drops the rest, with a
  count for the metrics of PR 4.
- **Latency histogram**: seventeen base-two buckets from 1 ms to 65 536 ms plus overflow; a
  percentile is the upper bound of the bucket where the cumulative count crosses the rank,
  capped by the observed maximum, so a single 10 ms call reports 10 ms and not 16 ms.
- **Window**: `GraphWindow` accumulates per-node versions and placements and per-edge metrics
  in insertion order; `freeze(end)` copies them into the immutable snapshot. `GraphBuilder`
  owns the current window, rejects spans of another environment (one snapshot belongs to one
  environment), applies the edge observations and the sweep results, and rotates the window on
  `freeze()`.
- **Queue and worker**: the custom ring buffer is replaced by `SpanQueue` over
  `ArrayBlockingQueue`. The old buffer was written for one producer, but gRPC delivers export
  calls on several threads, and it spun the consumer thread at full speed when idle. The bounded
  queue is thread-safe, `offer` never blocks the OTLP caller (rejections are counted, N2), and
  the worker blocks in `poll` with a 50 ms timeout instead of spinning. The power-of-two rule
  of `buffers.ring-size` is gone with the ring. `GraphWorker` is the single owner of the window:
  it drains batches of up to 512 spans, serves freeze requests (`requestFreeze()` returns a
  future that the worker completes on its own thread; concurrent requests share one future)
  and sweeps the pending index every 10 s with the injected `InstantSource`, so TTL behaviour
  is tested with a mutable clock instead of real time.
- **Runtime**: the snapshot loop asks the worker for a freeze every interval and hands the
  snapshot to the transport client, which still discards it; PR 3 adds the queue, the
  protobuf mapping and the reconnecting session.
- **Removed**: `model`, `service.graph`, `service.processor`, `snapshot` and the `otlp` graph
  classes (`GraphAggregator`, `NodeExtractor`, `NodeDescriptor`, the two `GraphNode`s,
  `GraphSnapshot`, `LatencyHistogram`, `SpanPipeline`, `SpanRingBuffer`); A5 and A11 are closed,
  A3 is closed for edges (node versions and deployments are recorded too).
- **Tests**: every rule of the edge table, both arrival orders, the pending index with a fixed
  clock, histogram percentiles, window accumulation and immutability, the worker's freeze
  hand-off and sweep with a mutable clock, queue rejection. The ratchet moves to branch 0.84
  (line and method sit at the 0.85 default).

### PR 3: snapshot publisher, reconnecting session, contract cleanup (ARCHI-40)

What landed and the decisions behind it:

- **Protobuf mapping** (`publish.SnapshotProtoMapper`): one static function from the
  immutable `GraphSnapshot` to the `GraphSnapshot` message; node types and edge kinds are
  exhaustive switches over the sealed types, versions and deployments are emitted sorted so
  two snapshots of the same window compare equal byte for byte.
- **Snapshot queue** (`publish.SnapshotQueue`): a bounded deque (`snapshot.queue-size`,
  default 64). `offer` drops the oldest entry when full and counts it; `requeue` puts a
  snapshot whose send failed back at the front and, if the queue is full meanwhile, drops that
  snapshot as the oldest one. The queue lives in the runtime, not in a session, so it survives
  reconnects (acceptance: a control plane restart loses at most `queue-size` windows, all
  counted).
- **Session** (`controlplane.ControlPlaneSession`): one gRPC stream per session. It registers,
  then runs three subtasks in a structured scope: the publisher drains the queue and sends
  snapshots, the heartbeat task sends `Heartbeat` at the interval the control plane announces
  in its `ConfigUpdate` (default 30 s), and a third task waits for the stream to end. A stream
  completion ends the session normally, a stream error or a failed send fails it; either way
  the scope cancels the other tasks and the channel is closed. Inbound commands update the
  heartbeat interval and count acknowledged and rejected snapshots (`PublisherStats`).
- **Supervisor** (`controlplane.ControlPlaneSupervisor`): runs sessions one after another,
  sleeps `control-plane.retry-seconds` between them and never gives up until the agent is
  interrupted. The previous runtime stopped supervising after the first failed session, which
  is why an agent started before its control plane never recovered (A2 in full).
- **Runtime**: the snapshot loop freezes the window every interval and offers the result to the
  queue; the supervisor, the worker and the receiver are the four tasks of the runtime scope.
  `TransportClient` shrank to `open` and `close`; the channel is built per session by
  `ControlPlaneClientFactory`.
- **Contract**: `graph_batch`, `GraphBatch`, `GraphNode` and `GraphEdge` are removed from
  `architrace-agent.proto`; field number 2 and the name are reserved. The control plane no
  longer has a branch for the deprecated payload. No released agent ever sent a batch, so
  nothing breaks ([ADR 0008](../../adr/0008-contract-first-apis/) allows this before the first
  release).
- **Removed from the agent**: the old `session`, `inbound` and `outbound` packages, the Guice
  wiring of control message handlers (the injector now runs without a module), the lifecycle,
  registration and bootstrap services, and the server-side `ControlPlaneServiceImpl` and
  `ControlPlaneRegistry` (A6).
- **Tests**: a `StubControlPlane` over in-process gRPC records what the agent sends and answers
  with config updates, acks or rejections, completes or fails the stream on demand. The session
  test covers registration, publishing, acknowledgement, heartbeats, rejection counting, stream
  failure and requeue; the supervisor test covers reconnects after failures and after a server
  completion; the runtime test is the end-to-end check of the M1 plan: OTLP traces exported to
  the real receiver come out as an acknowledged `GraphSnapshot` with the expected edge.

### PR 4: metrics endpoint, drop reporting, load test (ARCHI-41)

What landed and the decisions behind it:

- **Counters where the events happen, meters bound later.** The receiver counts normalised
  spans, the span queue its rejections, the graph builder foreign and evicted spans and the
  sizes of the current window, the pending index its held spans, the snapshot queue its
  drops, the publisher its published, acknowledged and rejected snapshots, its ended sessions
  and the connection flag. `AgentMetrics` binds these sources to a `MeterRegistry` as
  `FunctionCounter`s and `Gauge`s, so the hot path touches `LongAdder`s and volatile ints and
  never a Micrometer object. Counts that only the worker thread writes (`pending`, `active
  nodes`, `active edges`) are published as volatile values after each batch, sweep and freeze.
- **Endpoint** (`MetricsServer`): the JDK `HttpServer` on `metrics.port` with virtual-thread
  handlers. `/metrics` returns the Prometheus text format from `PrometheusMeterRegistry`,
  `/health` returns `{"status":"UP","controlPlane":"CONNECTED|DISCONNECTED"}` with status 200
  (liveness; the control plane state is information, not a failure), anything else 404. It is
  the sixth task of the runtime scope and stops with the agent.
- **Rate-limited logging by construction** (`DropReporter`): a runtime task wakes every 10 s,
  computes the delta of every loss counter since its previous report and logs one `WARN` line
  only when something was lost. No per-event logging, no throttle state in the hot path.
- **Load test** (`GraphWorkerLoadTest`, tag `load`): a producer offers client/server pairs at
  10 000 spans/s across 50 services while the worker builds the graph and a freeze is
  requested every second; it asserts no rejections, an empty pending index and a heap below
  512 MB after a GC. It is excluded from `test` and run with
  `./gradlew :architrace-agent:loadTest -Pload.seconds=600` for the ten-minute acceptance run;
  the default is 10 s. A 5 s run on the development machine: 50 000 spans, 0 rejected,
  250 edges, 10 MB heap.
- Dependencies: `micrometer-core` and `micrometer-registry-prometheus` 1.17.1, the version
  Spring Boot manages for the control plane.

### PR 5: formatter, acceptance run, closing (ARCHI-42)

What landed and the decisions behind it:

- **Formatter on.** `java.format.enabled=false` leaves `architrace-agent/gradle.properties`;
  palantir-java-format now runs on the whole module like everywhere else. The files written
  during M1 were already in that format, so the reflow touches only the survivors of the old
  code base (`MainApp`, the CLI, `BuildVersion`, the OTLP receiver and server,
  `GrpcAddressParser` and their tests). The SonarCloud new-code gate sees those lines as
  changed; they are covered by the existing tests.
- **Acceptance run.** `./gradlew :architrace-agent:loadTest -Pload.seconds=600` on the
  development machine: 6 000 000 spans in 600 s at 10 000 spans/s, 0 rejected, 0 pending at the end, 600 snapshots, 30 000 edges, 20 MB heap after a GC (`-Xmx512m`), no growth across the run.
- **Exceptions closed.** The agent module has no coverage ratchet and no formatter switch any
  more; `AGENTS.md` no longer lists them. Every defect of the inventory (A1–A12) is addressed:
  A1 and A2 by the publisher and the supervisor, A3 by the span timing and the edge metrics,
  A4 by the attribute mapping, A5 and A6 by the removals, A7 by configuration v2, A8 by
  `dry-run`, A9 by the rewritten tests, A10 by Spotless, A11 by the pending index with TTL,
  A12 by the counted queue rejection.

## Acceptance criteria review

- Standard OTel instrumentation yields service, database, topic and external nodes with sync,
  publish and consume edges and metrics: covered by `EdgeBuilderTest`, `SpanNormaliserTest`
  and the end-to-end `AgentRuntimeServiceTest`; the demo stack check is part of M7.
- 10 000 spans/s sustained: the `loadTest` task, see PR 5.
- A control plane restart loses at most `queue-size` snapshots, all counted:
  `ControlPlaneSupervisorTest`, `SnapshotQueueTest`.
- Coverage ≥ 85 %, no reflection, no dead classes: the module runs on the project defaults.

## Risks and open points

- Pairing relies on `parentSpanId`; instrumentation that breaks context propagation produces
  external edges instead of service edges, which is the honest result.
- Topic naming across brokers (Kafka topic vs RabbitMQ exchange/queue) is normalised by
  `messaging.system` + destination; refinements are post-MVP.
