---
title: Docker demo
description: Run the full stack with one command, see two environments with drift and findings, and know what you are looking at.
---

The demo in `demo/` starts Architrace with two environments of a small shop: DEV and STAGE
run the same four services, with deliberate differences, so the map, the drift and the
findings have something to show within a few minutes.

## What runs

| Service | Purpose |
|---------|---------|
| `postgres`, `control-plane` | the control plane with its store; UI and Query API on `8085` |
| `agent-dev`, `agent-stage` | one agent per environment (an agent serves exactly one environment; spans of another are dropped) |
| `otel-collector` | one OpenTelemetry Collector receiving every service's traces and routing them to the agent of their `deployment.environment.name` |
| `redpanda`, `redpanda-init` | a Kafka-compatible broker with the topic `order-events` |
| `demo-db` | a PostgreSQL database shared by two demo services |
| `payments` | an uninstrumented endpoint reachable as `api.payments.example`, which shows up as an external host |
| `dev-gateway`, `dev-order`, `dev-inventory`, `dev-notification` | the DEV environment |
| `stage-gateway`, `stage-order`, `stage-inventory`, `stage-notification` | the STAGE environment |
| `traffic` | a request every second to both gateways |

The demo services are one small Flask application (`demo/services/app.py`) whose role, name,
version, domain and environment come from the environment variables in `docker-compose.yml`;
the OpenTelemetry SDK with the standard instrumentations produces the spans.

```text
gateway ──sync──> order ──sync──> inventory ──sync──> demo-db
                  │  │                └──sync──> order (/status, DEV only)
                  │  ├──sync──> demo-db
                  │  ├──sync──> api.payments.example (external)
                  │  └──publish──> order-events ──consume──> notification
```

DEV and STAGE differ on purpose: in STAGE `inventory` does not call `order` back and `order`
runs version 1.4.0 instead of 1.5.0. In DEV the rules therefore find a cyclic dependency
(`order` and `inventory`), a shared database (`shop`), a wide blast radius of the database,
of the external host and of the topic (each impairs three of four services, because `order`
sits on every request path) and an unknown external host (`api.payments.example`); STAGE
keeps the shared database and the blast radius of the database.

## Run the demo

```bash
cd demo
docker compose up -d
```

The images of the control plane and the agent come from GitHub Container Registry
(`ghcr.io/architrace-intelligence/…:latest`); the demo services are built locally on the
first start. Open <http://localhost:8085>: the Projects list shows the project `demo` with the
scopes `DEV` and `STAGE` as soon as the first snapshots arrive (the agents report every 30 s
in the demo). Then:

1. Open **DEV** to see the service map; select `order` and choose the **Impact** lens.
2. Choose **Findings** in the navigation rail for the findings and their evidence.
3. Choose **Drift**: STAGE against DEV shows the missing dependency and the version
   difference.

External hosts appear about twenty seconds after the first request: a client span without a
server span waits `buffers.pending-ttl-seconds` (20 s in `demo/agent-dev.yaml`, 120 s by
default) before the agent treats its address as an external host.

To run the demo against images built from this repository instead of the published ones:

```bash
./gradlew :architrace-control-plane:bootJar :architrace-agent:assemble
docker build -t architrace-local/control-plane architrace-control-plane
docker build -t architrace-local/agent architrace-agent
cd demo && ARCHITRACE_CONTROL_PLANE_IMAGE=architrace-local/control-plane ARCHITRACE_AGENT_IMAGE=architrace-local/agent docker compose up -d
```

`ARCHITRACE_UI_PORT`, `DEMO_DEV_PORT` and `DEMO_STAGE_PORT` move the published ports
(`8085`, `8080`, `8081`) when they are taken.

## Demo files

- `demo/docker-compose.yml`: every service with its environment variables.
- `demo/otel-collector.yaml`: the OTLP receiver and the routing connector that sends DEV and
  STAGE traces to their agent.
- `demo/agent-dev.yaml`, `demo/agent-stage.yaml`: the two agent configurations (project,
  environment, cluster, control plane address, short snapshot interval and pending TTL).
- `demo/services/`: the demo application, its dependencies and image.

## From a script

```bash
curl -s http://localhost:8085/api/v1/scopes | jq .
curl -s 'http://localhost:8085/api/v1/scopes/demo/DEV/demo/findings' | jq '.[].title'
```

## Stop the demo

```bash
docker compose down -v
```

`-v` also removes the control plane's database volume, so the next start begins empty.
