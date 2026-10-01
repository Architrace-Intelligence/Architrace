---
title: 0001. PostgreSQL with Liquibase for the control plane store
description: The control plane persists topology in PostgreSQL; schema changes are Liquibase changelogs.
---

Status: accepted
Date: 2026-10-01

## Context

The control plane must persist graph snapshots, keep the current graph per environment and
retain history for drift comparison (requirements F6, F9, N3). The data is relational in shape:
environments, services, dependencies, snapshots, findings, all keyed by stable identifiers and
queried by time range. Schema evolution has to be automatic on start-up and reviewable in pull
requests. The maintainer's target environment runs PostgreSQL and the team already operates
Liquibase.

## Decision

- PostgreSQL is the only persistent store of the control plane.
- Liquibase owns the schema. Changelogs live in the control-plane module, one changeset per
  change, applied on application start. A changeset is never edited after it has been merged.
- The data access technology (Spring Data JDBC or JPA) is decided separately in the control
  plane design.

## Consequences

- Integration tests run against a real PostgreSQL through Testcontainers; no embedded database.
- The demo compose stack and the deployment documentation include PostgreSQL.
- Every PR that touches the schema ships a Liquibase changeset and the matching tests.
- Graph queries that outgrow SQL (deep traversals, path finding) are computed in the service
  layer on loaded snapshots, not pushed to a graph database.

## Alternatives considered

- **Flyway**: simpler SQL-only migrations; rejected because the maintainer standardises on
  Liquibase.
- **Graph database (Neo4j and similar)**: natural for topology, rejected for the MVP: extra
  infrastructure, snapshot and diff workloads are served well by relational tables.
- **Embedded database (H2, SQLite)**: zero setup, rejected because it would not match
  production and would hide SQL dialect issues.
