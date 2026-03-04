---
title: Docker Demo
description: Run end-to-end architecture intelligence with sample services.
---

## Demo topology

`otel-test-app/docker-compose.yml` starts:

- `control-plane` (`8085`, `9090`)
- `agent` (`4319`)
- `otel-collector` (`4317`, `4318`)
- `service-a`, `service-b`, `service-c` (Python demo services)

## Run demo

```bash
cd otel-test-app
docker compose build
docker compose up -d
```

Generate traffic:

```bash
curl http://localhost:8080/
```

Expected response:

```text
A -> B -> C
```

## Demo config files

- `otel-test-app/architrace-agent.yaml`: agent runtime config.
- `otel-test-app/otel-collector-config.yaml`: forwards traces to `agent:4319`.

## Stop demo

```bash
docker compose down
```
