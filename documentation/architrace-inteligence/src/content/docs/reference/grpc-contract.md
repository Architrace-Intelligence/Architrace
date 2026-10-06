---
title: gRPC Contract
description: Shared protobuf contract for agent and control-plane communication.
---

Source: `architrace-api/src/main/proto/architrace-agent.proto`, package `architrace.controlplane.v1`.
Both sides are generated from this file ([ADR 0008](../../project/adr/0008-contract-first-apis/)).
Until the first release the contract may still change; afterwards changes within `v1` are additive.

## Service

- `Connect(stream AgentRegisterRequestedEvent) returns (stream ControlPlaneCommand)`: one
  long-lived stream per agent.
- `GetAgentHealth(AgentHealthRequest) returns (AgentHealthResponse)`: liveness of the agent
  with that name, as the control plane sees it.

## Agent to control plane

`AgentRegisterRequestedEvent` carries exactly one of:

| Payload | When | Content |
|---------|------|---------|
| `register` | first message of the stream | `agent_name`, `agent_version`, and the scope: `project`, `environment`, `cluster_id` |
| `snapshot` | every snapshot interval | `GraphSnapshot`: window `[window_start_epoch_ms, window_end_epoch_ms)`, `nodes[]`, `edges[]` |
| `heartbeat` | every heartbeat interval (default 30 s, `heartbeat.interval-seconds` of the config update) | `sent_at_epoch_ms` |

Field number `2` (`graph_batch`) is reserved: the deprecated batch messages were removed with
the agent pipeline (M1).

A `SnapshotNode` has `id`, `type` (`SERVICE`, `DATABASE`, `TOPIC`, `EXTERNAL`), `name`,
`versions[]`, `deployments[]` (`cluster`, `namespace`) and free-form `labels`. A
`SnapshotEdge` has `source_id`, `target_id`, `kind` (`SYNC`, `PUBLISH`, `CONSUME`) and
`EdgeMetrics`: `calls`, `errors` and the latency percentiles `p50_micros`, `p95_micros`,
`p99_micros`, `max_micros` in microseconds (field numbers `3` to `6`, the former `*_millis`
values, are reserved since ARCHI-79).
Node ids follow the [topology model](../../architecture/#topology-model); every edge must
reference nodes of the same snapshot.

## Control plane to agent

`ControlPlaneCommand` carries exactly one of:

| Payload | When | Content |
|---------|------|---------|
| `config_update` | once after a successful registration | `version` and a map; today `snapshot.interval-seconds` and `heartbeat.interval-seconds` |
| `snapshot_ack` | after a snapshot is stored | `window_end_epoch_ms`, `snapshot_id` |
| `snapshot_rejected` | after an invalid snapshot or an empty message | `window_end_epoch_ms`, `reason`; the stream stays open |

## Stream errors

The control plane closes the stream with a gRPC status when the conversation cannot continue:
`INVALID_ARGUMENT` for a registration with a blank name or scope, `FAILED_PRECONDITION` for a
snapshot or heartbeat before registration and for a second registration on the same stream.
The agent keeps its unsent snapshots in a bounded queue (`snapshot.queue-size`, oldest dropped
and counted) and opens a new session after `control-plane.retry-seconds`, re-registering first;
a snapshot whose send failed goes back to the front of the queue.

## Health payload

`AgentHealthResponse` has `live` (fewer than three heartbeat intervals since the agent was
last seen) and `last_seen_epoch_ms` (`0` for an unknown agent).
