---
title: 0009. Spring Data JDBC for control plane persistence
description: Aggregates are immutable records mapped with Spring Data JDBC; complex reads use explicit SQL.
---

Status: accepted
Date: 2026-10-01

## Context

The control plane stores snapshots, nodes, edges, agents and findings in PostgreSQL
([ADR 0001](./0001-postgresql-liquibase/)). The code style is data-oriented: immutable records,
no hidden state, explicit queries. Reads are mostly "load a snapshot's nodes and edges" and
"latest snapshot per agent", writes are batch inserts.

## Decision

- Spring Data JDBC with records as aggregates (`Snapshot` with its nodes and edges as one
  aggregate; `Agent`; `Finding`).
- Reads that do not fit the aggregate model (current graph selection, pagination, retention
  deletes) use `JdbcClient` with explicit SQL in a repository class.
- No lazy loading, no entity lifecycle, no dirty checking; a save is an explicit call.
- Liquibase owns the schema; mappings never create tables.

## Consequences

- Domain records stay free of persistence annotations except the minimal `@Id` / `@Table`
  on aggregate roots in the adapter layer, or a separate persistence record mapped by hand
  when that would leak.
- Batch inserts of thousands of rows per snapshot use `JdbcClient` batch operations.
- Developers write SQL for non-trivial queries, which keeps performance visible.

## Alternatives considered

- **JPA / Hibernate**: rich but mutable entity model, lazy loading and dirty checking
  conflict with the data-oriented style; rejected.
- **jOOQ**: excellent typed SQL, rejected for the MVP to avoid code generation from the live
  schema in the build; it remains an option if SQL volume grows.
- **Plain `JdbcTemplate` everywhere**: minimal, rejected because aggregates for snapshots are
  easier to test with Spring Data JDBC.
