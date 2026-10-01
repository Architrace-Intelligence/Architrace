---
title: M2. Control plane ingestion and storage
description: Snapshots from every agent are validated, persisted and merged into a current graph per environment.
---

Status: in progress (PR 1 of 3 delivered: schema and stores) · Order: 3 · Requirements: F5, F6, N3, N4, N7

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
gRPC AgentStreamService ──► IngestionService.ingest(agent, GraphBatch)
   validate (environment, window, ids) ──► map to Snapshot/Node/Edge records
   ──► SnapshotRepository.save (one transaction) ──► AgentRegistry.touch ──► Ack
```

- Registration creates or updates the `agent` row; heartbeats update `last_seen_at`;
  liveness is `now - last_seen_at < 3 × heartbeat interval`.
- Invalid batches are rejected with a gRPC `INVALID_ARGUMENT` and counted; the stream stays open.
- `ConfigUpdate` is sent once after registration with the server-side snapshot interval so the
  control plane can tune agents later.

### Current graph

`TopologyQuery.currentGraph(environment, at)`:

1. For each agent of the environment, select the latest snapshot with `window_end ≤ at`.
2. Load nodes and edges of those snapshots.
3. Merge: same node id → union of attributes (versions, deployments); same edge key → sum of
   calls and errors, max of latencies.
4. Return an immutable `TopologyGraph(environment, at, nodes, edges)`.

The query runs in SQL for selection and in Java for merging; graphs of a few thousand nodes
stay well under 100 ms.

### Retention and metrics

- `topology.retention-days` (default 30); a daily job deletes older snapshots in batches.
- Metrics: `snapshots_ingested`, `snapshots_rejected`, `agents_connected`,
  `topology_query_seconds`; Actuator health includes the database.

## Acceptance criteria

- Snapshots from two agents of the same environment produce one merged current graph.
- Restarting the control plane keeps all history; the agent reconnects and continues.
- Retention removes old snapshots without touching newer ones (integration test with a clock).
- Coverage ≥ 85 % on the module; integration tests run in CI against PostgreSQL.

## Delivery plan

1. GA baseline, PostgreSQL, Liquibase, first schema, Testcontainers wiring, compose service
   (ARCHI-26, delivered).
2. Protobuf v1 redesign, agent registry, gRPC stream service, ingestion with validation and
   persistence.
3. Current graph and scope queries, retention job, metrics, architecture page update.

## Risks and open points

- Spring gRPC 1.0 and Spring Boot 4 compatibility matrix must be checked in PR 1.
- Very chatty agents (short intervals, many agents) are mitigated by batching inserts; the
  schema already avoids per-node rows for metrics history.
