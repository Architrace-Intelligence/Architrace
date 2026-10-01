---
title: "0008. Contract-first APIs: protobuf for agents, OpenAPI for the Query API"
description: Both contracts are authored by hand in architrace-api and all server and client code is generated from them.
---

Status: proposed
Date: 2026-10-01

## Context

Two interfaces cross process boundaries: agent ↔ control plane (streaming, high volume) and
UI or scripts ↔ control plane (request and response, human-readable). The UI is a separate
codebase that needs a typed client, and the project promises a stable API to users.

## Decision

- **Agent ↔ control plane**: protobuf over gRPC, package `architrace.controlplane.v1`, source
  of truth in `architrace-api`. The current messages are redesigned in place because nothing
  has been released; from the first release on, changes within `v1` are additive only, and a
  breaking change creates `v2` alongside.
- **Query API**: an OpenAPI 3.1 document in `architrace-api` is the source of truth. The
  Gradle build generates Spring server interfaces and models; the UI build generates
  TypeScript types from the same document. springdoc serves the document and Swagger UI.
- Both generated outputs are build artifacts, never committed.

## Consequences

- API changes are reviewed as diffs of the contract files, before any implementation.
- The control plane cannot compile if a controller drifts from the document; the UI cannot
  type-check if it uses a removed field.
- Writing the document by hand costs some time per endpoint; examples and descriptions in the
  document double as the API reference page.

## Alternatives considered

- **Code-first OpenAPI from annotations**: less typing, rejected because the document is
  produced at runtime, so the UI client would be generated from a running server and drift
  silently.
- **gRPC-web or GraphQL for the UI**: rejected for the MVP; REST with OpenAPI has the widest
  tooling and is easiest to script against.
