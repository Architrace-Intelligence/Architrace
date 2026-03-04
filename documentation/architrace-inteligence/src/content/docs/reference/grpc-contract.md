---
title: gRPC Contract
description: Shared protobuf contract for agent and control-plane communication.
---

Source: `architrace-api/src/main/proto/architrace-agent.proto`

## Package

`architrace.controlplane.v1`

## Service

- `Connect(stream AgentRegisterRequestedEvent) returns (stream ControlPlaneCommand)`
- `GetAgentHealth(AgentHealthRequest) returns (AgentHealthResponse)`

## Agent to control-plane messages

- `AgentRegisterRequestedEvent.register`
- `AgentRegisterRequestedEvent.graph_batch`

## Control-plane to agent messages

- `ControlPlaneCommand.config_update`

## Graph payload

- `GraphBatch.agent_name`
- `GraphBatch.observed_at_epoch_ms`
- `GraphBatch.nodes[]`
- `GraphBatch.edges[]`

`GraphEdge` tracks:

- `source_node_id`
- `target_node_id`
- `operation`
- `call_count`

## Health payload

`AgentHealthResponse` includes:

- `live`
- `last_seen_epoch_ms`
