---
title: M7. Packaging and demo
description: Container images, a one-command demo with two environments, and guides to run Architrace for real.
---

Status: in progress (images delivered by M0 PR 4, ARCHI-36; demo stack and guides pending) · Order: 8 · Requirements: F12, N8

## Goal

Anyone can run the full stack with one command, see two environments with drift and findings,
and then deploy the same images in their own infrastructure.

## Scope

In: Dockerfiles, image publishing, demo stack with two environments and a message broker,
getting-started and deployment guides.

Out: Helm chart, Kubernetes manifests, native image (post-MVP).

## Design

### Images

- `architrace-agent/Dockerfile` and `architrace-control-plane/Dockerfile`: single stage over the
  prebuilt jar, Eclipse Temurin 25 JRE on Alpine pinned by digest, non-root user, container-aware
  JVM flags, `HEALTHCHECK`, OCI labels with version and source (delivered in M0 PR 4). The
  control plane reads `ARCHITRACE_DB_URL`, `ARCHITRACE_DB_USERNAME` and `ARCHITRACE_DB_PASSWORD`;
  the agent expects its YAML at `/config/architrace-agent.yaml` and announces its OTLP port
  through `ARCHITRACE_OTLP_PORT` for the health check.
- Published by the main pipeline (M0) to `ghcr.io/architrace-intelligence/architrace-agent`
  and `…/architrace-control-plane`, tags `X.Y.Z`, `latest`, `sha-<short>`; Trivy scan before
  push.

### Demo stack (`demo/`)

Replaces `otel-test-app/`.

| Service | Purpose |
|---------|---------|
| `postgres` | control plane store |
| `control-plane` | ingest, API, UI on `8085` |
| `agent` | one agent serving both demo environments (environment from resource attributes) |
| `otel-collector` | receives from demo services, exports to the agent |
| `redpanda` | Kafka-compatible broker for publish and consume edges |
| `demo-db` | PostgreSQL used by a demo service, producing a database node |
| `dev-*` services | DEV environment: order, inventory, notification (sync chain, one topic, one database) |
| `stage-*` services | STAGE environment: same set minus one dependency, one service on an older version |
| `traffic` | generates requests continuously so metrics are non-zero |

Demo services stay small (Python with the OTel SDK, standard semantic conventions); the
differences between DEV and STAGE are deliberate so drift and findings are visible out of the
box.

### Guides

- Getting started: `docker compose up`, open the UI, what to click first.
- Deployment: environment variables, ports, persistence, sizing, running one agent per
  cluster, collector configuration snippet.
- Reference pages for agent configuration and control plane properties regenerated from the
  implemented options.

## Acceptance criteria

- `docker compose up` on a clean machine shows two environments, a drift between them and at
  least two findings within three minutes.
- Images start as non-root, pass Trivy with no critical findings and respond to health checks.
- The deployment guide is sufficient to run the agent against a real collector.

## Delivery plan

1. Dockerfiles and image publishing: done (M0 PR 4, ARCHI-36).
2. Demo stack with two environments, broker and database; traffic generator.
3. Guides and reference pages; final architecture page update for the MVP.

## Open points

From the real-data round ([Requirements §9](../../requirements/#9-backlog-from-the-first-real-data-test-round)):

- B5: the agent `HEALTHCHECK` defaults to port `4317` while the agent listens on `4319`; the
  check moves to `/health` on the metrics port and one default port is shared by the
  Dockerfile, the configuration and the docs.
- B6: the deployment guide gets a section on instrumenting Spring Boot services: the
  environment variables for the OpenTelemetry starter, what the starter covers (HTTP, JDBC,
  R2DBC, Spring Kafka) and what needs the Java agent (Kafka Streams, reactor-kafka, raw Kafka
  clients, Lettuce, OkHttp), with the note that Prometheus metrics stay untouched.
- B7: the demo configuration uses a shorter pending TTL so external hosts appear within the
  first minute, and the setting documents the delay.
