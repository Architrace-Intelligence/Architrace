---
title: "0007. Topology model: identity, snapshots and time"
description: How nodes and edges are identified across environments, what a snapshot is, and how "the graph at time T" is defined.
---

Status: accepted
Date: 2026-10-01

## Context

Drift comparison only works if the same service in DEV and PROD has the same identity, while
deployments, versions and traffic differ per environment. History must be queryable by time
without storing raw spans (requirements F4, F6, F9).

## Decision

### Identity

| Node type | Id | Source attributes |
|-----------|----|-------------------|
| service | `service:{domain}/{name}` | `service.namespace`, `service.name` |
| database | `db:{system}/{namespace}` | `db.system`, `db.namespace` |
| topic | `topic:{system}/{name}` | `messaging.system`, `messaging.destination.name` |
| external | `ext:{address}` | `server.address` of an unmatched client span |

- Ids never contain the environment. Environment, versions, clusters and namespaces are
  attributes of the node within a snapshot.
- Edge key: `(sourceId, targetId, kind)` with kind `sync`, `publish` or `consume`.
- Environment comes from `deployment.environment.name` when present, else from the agent's
  configured default. One snapshot belongs to exactly one environment.

### Snapshots and time

- A snapshot is an immutable record of one agent's window `[start, end)` for one environment:
  nodes with attributes and edges with window metrics.
- The graph of an environment at time `T` is the union of the latest snapshot per agent with
  `end ≤ T`; duplicate nodes merge attributes, duplicate edges sum counts and take max
  latencies.
- Metrics are per window, not cumulative; the control plane derives trends from the series.
- Snapshots are retained for a configurable period (default 30 days) and then deleted.

## Consequences

- Drift is a set comparison on ids plus attribute comparison on matched nodes; it needs no
  reconciliation table.
- Renaming a service changes its identity and shows as removed plus added, which is the
  truthful answer.
- Services that stop sending traffic disappear from the current graph after their agent's
  next window; "last seen" is derivable from the snapshot series.

## Alternatives considered

- **Per-span or per-trace storage with graph computed on read**: maximal flexibility, rejected
  for volume and because the agent already has the data to reduce.
- **Continuous graph with per-edge TTL and no history**: simpler storage, rejected because
  release drift needs points in time.
- **Instance-level identity (pod, host)**: rejected for the MVP; deployments are attributes,
  not nodes.
