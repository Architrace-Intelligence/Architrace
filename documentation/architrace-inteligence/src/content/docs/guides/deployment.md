---
title: Installing in production
description: Step by step from an empty cluster to a filled service map - PostgreSQL, the control plane, one agent per environment, the collector route, sizing, security and upgrades, with Docker and Kubernetes examples.
---

Architrace in production is three things: a **PostgreSQL** database, one **control plane**
and **one agent per environment**. Your services keep their code; they send traces to the
OpenTelemetry Collector you probably already run, and the collector forwards them to the agent
of that environment. This page walks through the installation in order; each step ends with a
check you can run before moving on. Plan about an hour for the first environment.

```text
services ──OTLP──► OpenTelemetry Collector ──OTLP/gRPC :4319──► agent (one per environment)
                                                                   │ gRPC :9090
users ──HTTPS──► reverse proxy ──HTTP :8085──► control plane ◄─────┘
                                                   │ JDBC
                                               PostgreSQL
```

| Component | Image or requirement | Listens on | Talks to |
|-----------|----------------------|------------|----------|
| PostgreSQL | 15 or newer, yours | `5432` | nothing |
| Control plane | `ghcr.io/architrace-intelligence/architrace-control-plane` | `8085` HTTP (UI, Query API, Swagger UI, actuator), `9090` gRPC (agents) | PostgreSQL |
| Agent | `ghcr.io/architrace-intelligence/architrace-agent` | `4319` OTLP/gRPC (traces in), `9464` HTTP (`/health`, `/metrics`) | control plane `9090` |
| Collector | OpenTelemetry Collector, yours | `4317` / `4318` | agent `4319` |

## Before you start

- A container runtime (Docker, or Kubernetes 1.27 or newer with `kubectl`).
- PostgreSQL 15 or newer, reachable from the control plane. A managed instance is fine.
- Network paths: services to the collector, the collector to the agent on `4319`, the agent to
  the control plane on `9090`, users to the control plane on `8085` through your reverse proxy.
- A version to pin. Every release publishes both images under the same `X.Y.Z` tag; use the
  same tag for the control plane and the agents and move both together. `latest` is for trying
  things out, not for production.
- The images run as a non-root user, set the JVM heap to 75 % of the container memory limit
  (`-XX:MaxRAMPercentage=75.0`) and exit on `OutOfMemoryError` so the orchestrator restarts a
  broken instance. Give every container a memory limit; the JVM sizes itself from it.

## Step 1: Prepare the database

Create a database and a user that owns it. The control plane applies its schema with Liquibase
on every start, so the user needs the right to create and alter tables in that database and
nothing beyond it.

```sql
CREATE ROLE architrace LOGIN PASSWORD 'change-me';
CREATE DATABASE architrace OWNER architrace;
```

Sizing: one snapshot per agent per minute, a few megabytes per agent and day for a graph of a
hundred services. Snapshots older than thirty days are deleted nightly (`architrace.topology.retention.period`),
so the database stops growing after a month. Back it up like any other small PostgreSQL
database; the schema is recreated from scratch by Liquibase, the data is not.

**Check:** `psql "postgresql://architrace:change-me@<host>:5432/architrace" -c 'select 1'` answers.

## Step 2: Run the control plane

The control plane needs the database connection and nothing else. Everything is configured
through environment variables.

| Variable | Required | Default | Meaning |
|----------|----------|---------|---------|
| `ARCHITRACE_DB_URL` | yes | `jdbc:postgresql://localhost:5432/architrace` | JDBC URL of the database from step 1 |
| `ARCHITRACE_DB_USERNAME`, `ARCHITRACE_DB_PASSWORD` | yes | `architrace`, `architrace` | the owner from step 1 |
| `ARCHITRACE_TOPOLOGY_RETENTION_PERIOD` | no | `30d` | how long snapshots are kept |
| `ARCHITRACE_INGESTION_SNAPSHOTINTERVAL` | no | `60s` | how often every agent publishes a snapshot; the control plane pushes this value to the agents, so set it here, not in the agent files |
| `ARCHITRACE_TOPOLOGY_PLATFORMHOSTS` | no | empty | host names of platform services every workload calls (feature flags, configuration server); shown once on the map and never reported as unknown |
| `ARCHITRACE_RULES_UNKNOWNEXTERNAL_ALLOWLIST` | no | empty | host names of external systems you know about; everything else raises `unknown-external` |
| `SERVER_PORT`, `SPRING_GRPC_SERVER_PORT` | no | `8085`, `9090` | HTTP and gRPC ports |

Every property on the [configuration reference](../../reference/configuration/#control-plane)
works the same way: upper case, dots and dashes removed (`architrace.rules.fan-in-hub.max-callers`
is `ARCHITRACE_RULES_FANINHUB_MAXCALLERS`). Memory: start with a **1 GiB** limit (768 MiB heap)
for up to a few hundred services and a handful of agents; the control plane holds the latest
graph of every scope in memory while it evaluates the rules and serves the UI from the database
otherwise. Run **one replica**: the retention job and the rule evaluation are built for a single
instance in this version, so scale it up, not out.

**Docker:**

```bash
docker network create architrace

docker run -d --name architrace-control-plane --network architrace \
  --memory 1g --restart unless-stopped \
  -p 127.0.0.1:8085:8085 \
  -e ARCHITRACE_DB_URL=jdbc:postgresql://db.example.internal:5432/architrace \
  -e ARCHITRACE_DB_USERNAME=architrace \
  -e ARCHITRACE_DB_PASSWORD=change-me \
  ghcr.io/architrace-intelligence/architrace-control-plane:0.6.0
```

**Kubernetes:** a Secret for the database, a Deployment with one replica and a Service that
exposes both ports inside the cluster.

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: architrace-db
  namespace: architrace
stringData:
  ARCHITRACE_DB_URL: jdbc:postgresql://postgres.database.svc:5432/architrace
  ARCHITRACE_DB_USERNAME: architrace
  ARCHITRACE_DB_PASSWORD: change-me
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: architrace-control-plane
  namespace: architrace
spec:
  replicas: 1
  selector:
    matchLabels:
      app: architrace-control-plane
  template:
    metadata:
      labels:
        app: architrace-control-plane
    spec:
      containers:
        - name: control-plane
          image: ghcr.io/architrace-intelligence/architrace-control-plane:0.6.0
          envFrom:
            - secretRef:
                name: architrace-db
          env:
            - name: ARCHITRACE_TOPOLOGY_RETENTION_PERIOD
              value: 30d
          ports:
            - name: http
              containerPort: 8085
            - name: grpc
              containerPort: 9090
          resources:
            requests:
              cpu: 250m
              memory: 1Gi
            limits:
              memory: 1Gi
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: http
            initialDelaySeconds: 20
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: http
            initialDelaySeconds: 60
            periodSeconds: 20
---
apiVersion: v1
kind: Service
metadata:
  name: architrace-control-plane
  namespace: architrace
spec:
  selector:
    app: architrace-control-plane
  ports:
    - name: http
      port: 8085
    - name: grpc
      port: 9090
```

Publish port `8085` to your users through the reverse proxy or Ingress you use for internal
tools, **with authentication in front of it**: the control plane has no user accounts in this
version, and whoever reaches the port sees every graph. Port `9090` stays inside the network;
only the agents need it.

**Check:** `curl -s http://<control-plane>:8085/actuator/health` answers `{"status":"UP"}` once
Liquibase has created the schema (the first start takes a few seconds longer); the UI opens on
the same port and shows an empty Projects list.

## Step 3: Run one agent per environment

An agent registers with a **scope**: `project` × `environment` × `cluster`. It accepts the
traces of that environment and drops spans whose `deployment.environment.name` says otherwise,
so one agent per environment keeps the scopes clean; run one per cluster as well when the
clusters are separate scopes for you. An agent is stateless apart from the current window; if it
restarts, the next snapshot is complete again a minute later.

The agent reads one YAML file, mounted at `/config/architrace-agent.yaml`. Four keys are
required:

```yaml
project: webshop            # groups the environments on the Projects list
environment: PROD           # must equal the services' deployment.environment.name
cluster: eu-1               # must equal the services' k8s.cluster.name, if they send one
agent:
  name: eu-1-prod           # unique per agent, shown on the Agents list
control-plane:
  server: architrace-control-plane.architrace.svc:9090
```

Optional keys worth knowing (every key is on the [configuration reference](../../reference/configuration/#agent)):

| Key | Default | When to change it |
|-----|---------|-------------------|
| `buffers.pending-ttl-seconds` | `120` | how long a client span waits for the matching server span; an external host appears on the map only after this wait, so `30` shows externals sooner in a landscape with few slow calls |
| `buffers.ring-size` | `65536` | the queue in front of the graph worker; raise it for bursts above ten thousand spans per second, watch `architrace_agent_spans_rejected_total` |
| `topics.ignore` | the Kafka Streams internal topics | add your own glob patterns for topics that are implementation detail, `[]` keeps every topic |
| `otlp.port`, `metrics.port` | `4319`, `9464` | when you must; set `ARCHITRACE_METRICS_PORT` too, the image health check probes it |
| `attribute-mapping.<field>` | the semantic conventions | when your services name an attribute differently, for example `team` instead of `service.namespace` |

Memory: a **512 MiB** limit (384 MiB heap) covers thousands of spans per second and a graph of
a few hundred services; the window, the pending index and the snapshot queue are bounded.

**Docker:**

```bash
docker run -d --name architrace-agent-prod --network architrace \
  --memory 512m --restart unless-stopped \
  -p 127.0.0.1:4319:4319 -p 127.0.0.1:9464:9464 \
  -v "$PWD/architrace-agent.yaml:/config/architrace-agent.yaml:ro" \
  ghcr.io/architrace-intelligence/architrace-agent:0.6.0
```

Validate a file without starting anything: the same image with the `dry-run` command prints
the effective configuration or every problem it found.

```bash
docker run --rm -v "$PWD/architrace-agent.yaml:/config/architrace-agent.yaml:ro" \
  ghcr.io/architrace-intelligence/architrace-agent:0.6.0 dry-run --config /config/architrace-agent.yaml
```

**Kubernetes:** a ConfigMap per environment, a Deployment and a Service that the collector can
reach by name.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: architrace-agent-prod
  namespace: architrace
data:
  architrace-agent.yaml: |
    project: webshop
    environment: PROD
    cluster: eu-1
    agent:
      name: eu-1-prod
    control-plane:
      server: architrace-control-plane.architrace.svc:9090
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: architrace-agent-prod
  namespace: architrace
spec:
  replicas: 1
  selector:
    matchLabels:
      app: architrace-agent-prod
  template:
    metadata:
      labels:
        app: architrace-agent-prod
    spec:
      containers:
        - name: agent
          image: ghcr.io/architrace-intelligence/architrace-agent:0.6.0
          ports:
            - name: otlp
              containerPort: 4319
            - name: metrics
              containerPort: 9464
          volumeMounts:
            - name: config
              mountPath: /config
              readOnly: true
          resources:
            requests:
              cpu: 250m
              memory: 512Mi
            limits:
              memory: 512Mi
          readinessProbe:
            httpGet:
              path: /health
              port: metrics
            initialDelaySeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /health
              port: metrics
            initialDelaySeconds: 30
            periodSeconds: 20
      volumes:
        - name: config
          configMap:
            name: architrace-agent-prod
---
apiVersion: v1
kind: Service
metadata:
  name: architrace-agent-prod
  namespace: architrace
spec:
  selector:
    app: architrace-agent-prod
  ports:
    - name: otlp
      port: 4319
    - name: metrics
      port: 9464
```

**Check:** `curl -s http://<agent>:9464/health` answers `{"status":"UP","controlPlane":"CONNECTED"}`,
`/api/v1/agents` on the control plane lists the agent as live, and the Projects list counts it
under its scope.

## Step 4: Route traces to the agent

Point the collector of the environment at the agent. The agent speaks **OTLP over gRPC only**,
without TLS, so the exporter needs `tls.insecure: true` and the collector should sit in the
same network as the agent (a service mesh or an Ingress with TLS termination in front of the
agent works too).

```yaml
exporters:
  otlp/architrace:
    endpoint: architrace-agent-prod.architrace.svc:4319
    tls:
      insecure: true

processors:
  resource/scope:
    attributes:
      - key: deployment.environment.name
        value: PROD
        action: upsert
      - key: k8s.cluster.name
        value: eu-1
        action: upsert

service:
  pipelines:
    traces/architrace:
      receivers: [otlp]
      processors: [resource/scope]
      exporters: [otlp/architrace]
```

The `resource/scope` processor is the best practice for the two attributes the agent keys on:
set the environment and the cluster once, at the collector of that environment, and no
service can get them wrong. Add the exporter to the existing traces pipeline next to your
tracing backend; the agent is one more destination, not a replacement.

One collector that serves several environments routes by resource attribute with the
`routing` connector instead, one pipeline and one exporter per environment; the
[demo](../docker-demo/) ships exactly that configuration. Services without a collector can
export straight to the agent with `OTEL_EXPORTER_OTLP_PROTOCOL=grpc` and
`OTEL_EXPORTER_OTLP_ENDPOINT=http://<agent>:4319`.

**Check:** `curl -s http://<agent>:9464/metrics | grep architrace_agent_spans` shows
`architrace_agent_spans_received_total` growing and `architrace_agent_spans_foreign_total` at
zero; a growing `foreign` counter means the services report a different environment than the
agent's `environment`.

## Step 5: Instrument the services

The agent only sees what the traces say, so the instrumentation decides what appears on the
map. [Instrumenting services](../instrumenting-services/) is the step-by-step guide: the
OpenTelemetry Java agent attached without a code change, the Spring Boot starter configured
through properties, the resource attributes Architrace needs, sampling, rollout and
troubleshooting.

## Step 6: Verify end to end

1. The Projects list shows the project with one scope per agent within one snapshot interval
   (60 s by default).
2. The service map of the scope shows the services with their databases, topics and external
   hosts after the first minute of traffic; a missing dependency is an instrumentation gap, see
   the [troubleshooting table](../instrumenting-services/#troubleshooting).
3. `/api/v1/agents` lists every agent as live with its version; the Projects list shows the
   live agent count per scope.
4. The Findings screen shows the first findings after the next evaluation (within 30 s of a
   snapshot).

From a script, the same checks against the [Query API](../../reference/query-api/):

```bash
curl -s http://<control-plane>:8085/api/v1/scopes
curl -s http://<control-plane>:8085/api/v1/agents
curl -s "http://<control-plane>:8085/api/v1/scopes/webshop/PROD/eu-1/graph"
```

## Step 7: Operate

**Monitor the agents.** Scrape `/metrics` on `9464` with Prometheus and alert on
`architrace_agent_controlplane_connected == 0` for longer than a few minutes and on
`rate(architrace_agent_spans_rejected_total[5m]) > 0`. The agent also logs one `WARN` line
every ten seconds while it loses anything, and nothing otherwise.

**Monitor the control plane.** `/actuator/health` for liveness, `/actuator/metrics` for the
JVM and HTTP metrics; `/api/v1/agents` tells which agent stopped reporting.

**Upgrade.** Pin the same release on both images. Upgrade the control plane first (Liquibase
migrates the schema on start, the agents reconnect and re-register on their own), then the
agents. A newer agent with an older control plane keeps working; the fields the older side
does not know are ignored. Release notes are on the
[releases page](https://github.com/Architrace-Intelligence/Architrace/releases).

**Secure.** Authentication in front of `8085`; `9090` reachable from the agents only; `4319`
reachable from the collectors only (a NetworkPolicy per port on Kubernetes, security groups
or a firewall elsewhere). The images run as a non-root user and need no privileges or
capabilities, so `runAsNonRoot: true` holds; with `readOnlyRootFilesystem: true` mount an
`emptyDir` at `/tmp`, where the JVM and the embedded server keep their working files.

**Tune.** Snapshot interval from the control plane (`ARCHITRACE_INGESTION_SNAPSHOTINTERVAL`,
a shorter interval shows changes sooner and stores more), retention from the control plane,
pending TTL and queue sizes from the agent file. Change one value at a time and watch the
counters above.

## Checklist

1. Database created, user owns it, reachable from the control plane.
2. Control plane up: `/actuator/health` is `UP`, the UI opens behind authentication.
3. One agent per environment with its file mounted: `/health` says `CONNECTED`.
4. The collector of each environment exports OTLP/gRPC to its agent and stamps the environment
   and the cluster.
5. Services instrumented; `spans_received` grows, `spans_foreign` stays at zero.
6. The map shows services, databases, topics and external hosts; findings appear.
7. Both images pinned to one release; memory limits set; metrics scraped.
