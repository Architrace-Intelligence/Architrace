---
title: M2. Control plane ingestion and storage
description: Snapshots from every agent are validated, persisted and merged into a current graph per environment.
---

Status: delivered in three PRs (schema and stores, ingestion, current graph and retention) · Order: 3 · Requirements: F5, F6, N3, N4, N7

## Goal

The control plane keeps the history of every environment's topology and can answer "what does
environment X look like at time T" from PostgreSQL.

## Scope

In: Spring Boot GA baseline, PostgreSQL with Liquibase, agent registry, snapshot ingestion,
current graph query, retention, metrics.

Out: REST endpoints (M3), diff (M5), rules (M6), authentication.

## Design

### Baseline

- Spring Boot and Spring gRPC pinned to GA releases, Maven Central only
  ([ADR 0010](../../adr/0010-spring-boot-ga/)).
- Spring Data JDBC for persistence ([ADR 0009](../../adr/0009-spring-data-jdbc/)), Liquibase
  YAML changelogs under `db/changelog/`, applied on start-up ([ADR 0001](../../adr/0001-postgresql-liquibase/)).
- Integration tests with Testcontainers PostgreSQL; `@DataJdbcTest` for repositories.

### Schema (first changelog)

| Table | Columns | Notes |
|-------|---------|-------|
| `agent` | id, name, agent_version, project, environment, cluster, first_seen_at, last_seen_at | unique (name, project, environment, cluster) |
| `snapshot` | id, agent_id, project, environment, cluster, window_start, window_end, received_at, node_count, edge_count | index (project, environment, cluster, window_end), index (agent_id, window_end) |
| `snapshot_node` | snapshot_id, node_id, type, name, attributes (jsonb) | pk (snapshot_id, node_id) |
| `snapshot_edge` | snapshot_id, source_id, target_id, kind, calls, errors, latency_p50, latency_p95, latency_p99, latency_max | pk (snapshot_id, source_id, target_id, kind) |

`attributes` holds versions, deployments (cluster, namespace) and free labels as a JSON document
so new node attributes do not need a migration.

The scope columns (`project`, `environment`, `cluster`) come from the UI design of the initial
release: the Projects list shows one row per scope and the map opens for one scope. The agent
reports its scope at registration; snapshots are denormalised with it so scope queries need no
join. Where the project value comes from on the agent side is an open point of the
[UI design](../ui-design/).

### Code shape (PR 1)

- `topology`: domain records without framework imports (`Scope`, `TimeWindow`, `TopologyNode`,
  `NodeAttributes`, `TopologyEdge`, `EdgeMetrics`, `Snapshot`, `Agent`, `AgentRegistration`)
  and the ports `SnapshotStore` and `AgentStore`.
- `topology.persistence`: Spring Data JDBC aggregates (`SnapshotRow` with node and edge rows,
  `AgentRow`), repositories, the `JsonDocument` ↔ `jsonb` converters, the Jackson codec for
  node attributes, and the adapters `JdbcSnapshotStore` and `JdbcAgentStore`.
- Tests: `@DataJdbcTest` slices and the application context run against PostgreSQL 17 in
  Testcontainers (`PostgresTestcontainers`, `@ServiceConnection`).

### Ingestion

```
gRPC AgentStreamService ──► AgentConnection (one per stream)
   register ──► IngestionService.register ──► AgentStore ──► ConfigUpdate
   snapshot ──► IngestionService.ingest(agent, GraphSnapshot)
      SnapshotMapper: window, typed nodes, edge kinds, metrics, unique ids, known endpoints
      ──► SnapshotStore.save (one transaction) ──► AgentStore.touch ──► SnapshotAck
   heartbeat ──► AgentStore.touch
```

- Registration creates or updates the `agent` row for the scope-qualified identity
  (name, project, environment, cluster); heartbeats and snapshots update `last_seen_at`;
  liveness (`AgentLiveness`) is `now - last_seen_at < 3 × heartbeat interval`.
- An invalid snapshot is answered with `SnapshotRejected` (reason) and counted; the stream
  stays open. A bidirectional stream cannot carry a gRPC status without closing, so the
  status codes are reserved for conversations that cannot continue: `INVALID_ARGUMENT` for a
  blank registration, `FAILED_PRECONDITION` for messages before registration or a second
  registration.
- `ConfigUpdate` is sent once after registration with `snapshot.interval-seconds` and
  `heartbeat.interval-seconds` (`architrace.ingestion.*`, defaults 60 s and 30 s).
- Metrics: `architrace.snapshots.ingested`, `architrace.snapshots.rejected`,
  `architrace.agents.connected`.
- The contract was extended additively in ARCHI-28 (`snapshot`, `heartbeat`, scope fields on
  `register`, `snapshot_ack`, `snapshot_rejected`); `graph_batch` is deprecated and rejected,
  and is removed together with the agent's old publisher in M1. See the
  [gRPC contract](../../../reference/grpc-contract/).

### Current graph

`TopologyQuery.currentGraph(scope, at)`:

1. Select the latest snapshot per agent of the scope with `window_end ≤ at`
   (`select distinct on (agent_id) … order by agent_id, window_end desc, id desc`), nodes and
   edges included.
2. Merge in Java (`GraphMerger`): same node id → union of versions, deployments and labels (the
   first agent wins on a conflicting label value); same edge key `(source, target, kind)` → sum
   of calls and errors, max of every latency.
3. Return an immutable `TopologyGraph(scope, at, nodes, edges)` with nodes sorted by id and
   edges by key.

`TopologyQuery.scopes()` lists every scope with a registered agent as a `ScopeSummary`: agents
and live agents (`AgentLiveness`), services, data streams (topics), namespaces (taken from the
deployments of the current graph) and the end of the newest snapshot window. The Projects list
of the initial UI release shows one row per summary.

Selection runs in SQL on the scope and window indexes, merging in Java; graphs of a few
thousand nodes stay well under 100 ms. The timer `architrace.topology.query` records it.

### Retention and metrics

- `RetentionJob` deletes snapshots whose window ended before `now - period`, in batches, and
  the cascading foreign keys remove their nodes and edges. Properties under
  `architrace.topology.retention`: `period` (default `30d`), `batch-size` (default `1000`),
  `cron` (default `0 0 3 * * *`, UTC).
- Metrics: `architrace.snapshots.ingested`, `architrace.snapshots.rejected`,
  `architrace.snapshots.deleted`, `architrace.agents.connected`, timer
  `architrace.topology.query`. Actuator exposes `/actuator/health` (database included) and
  `/actuator/metrics`.

### Code shape (PR 3)

- `topology`: `TopologyGraph`, `ScopeSummary`, the pure `GraphMerger`, the service
  `TopologyQuery`, the Micrometer facade `TopologyMetrics` and `TopologyConfiguration` (clock,
  scheduling, retention properties). `SnapshotStore` gains `latestPerAgent` and
  `deleteOlderThan`.
- `topology.retention`: `RetentionProperties` and the scheduled `RetentionJob`.
- Tests: `GraphMergerTest`, `TopologyQueryTest` and `RetentionJobTest` on the in-memory stores;
  `JdbcSnapshotStoreTest` for the two new queries; `TopologyIntegrationTest` (full context,
  fixed clock, PostgreSQL in Testcontainers): two agents produce one merged graph and retention
  keeps the recent history.

## Acceptance criteria

- Snapshots from two agents of the same environment produce one merged current graph.
- Restarting the control plane keeps all history; the agent reconnects and continues.
- Retention removes old snapshots without touching newer ones (integration test with a clock).
- Coverage ≥ 85 % on the module; integration tests run in CI against PostgreSQL.

## Delivery plan

1. GA baseline, PostgreSQL, Liquibase, first schema, Testcontainers wiring, compose service
   (ARCHI-26, delivered).
2. Protobuf v1 extended, agent registry, gRPC stream service, ingestion with validation and
   persistence, agent registers with its scope (ARCHI-28, delivered).
3. Current graph and scope queries, retention job, metrics, architecture page update
   (ARCHI-29, delivered).

## Acceptance criteria review

- Two agents of one scope produce one merged current graph: the current-graph query tests
  (nodes united by id, edges by source, target and kind) and the ingestion tests.
- A restart keeps the history and the agent reconnects: snapshots are rows in PostgreSQL, the
  agent side is covered by `ControlPlaneSupervisorTest` (M1); a demo-stack restart check is
  part of M7.
- Retention removes old snapshots only: the retention job test with an injected clock.
- Coverage ≥ 85 % and PostgreSQL integration tests in CI: the module runs on the project
  defaults with Testcontainers on the `build` job.

## Risks and open points

- Spring gRPC 1.0 and Spring Boot 4 compatibility: checked in PR 1 and settled by the
  dependency update ARCHI-35 (Spring Boot 4.1.1 with the Boot gRPC starter).
- Very chatty agents (short intervals, many agents) are mitigated by batching inserts; the
  schema already avoids per-node rows for metrics history.
