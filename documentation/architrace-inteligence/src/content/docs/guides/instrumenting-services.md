---
title: Instrumenting services
description: Make every dependency visible on the map without changing service code - the OpenTelemetry Java agent, the Spring Boot starter through properties, the resource attributes Architrace needs, sampling, rollout and troubleshooting.
---

Architrace reads traces and nothing else. A dependency appears on the map when a span
describes it, so the instrumentation of your services decides what you see. None of it needs
a code change: the OpenTelemetry Java agent attaches to any JVM from the command line, and the
Spring Boot starter is one dependency configured through properties. This page is the
step-by-step route for a team that runs Spring Boot services and wants the whole landscape on
the map after one rollout.

## Step 1: Choose how to instrument

| | OpenTelemetry Java agent | Spring Boot starter |
|---|---|---|
| How it attaches | `-javaagent:` flag, no build change | one dependency in the build, no code |
| Covers | Spring MVC and WebFlux, every common HTTP client (`RestTemplate`, `WebClient`, `RestClient`, OkHttp, Apache, JDK), JDBC and R2DBC, Spring Kafka, Kafka Streams, reactor-kafka, raw `KafkaProducer` and `KafkaConsumer`, Lettuce and Jedis, gRPC, and more | Spring MVC and WebFlux, `RestTemplate`, `WebClient`, `RestClient`, JDBC, R2DBC, Spring Kafka (`@KafkaListener`, `KafkaTemplate`) |
| Misses | nothing you are likely to run | Kafka Streams, reactor-kafka, raw Kafka clients, Redis clients, OkHttp and other third-party HTTP clients: those dependencies stay invisible |
| Cost | some seconds of start-up time, a small steady-state overhead | native, lighter |
| Choose when | you want the complete map with one mechanism for every service | the service only talks HTTP, SQL and Spring Kafka, or you cannot change the launch command |

**Recommendation:** the Java agent everywhere, switched on per service by an environment
variable. It is the one mechanism that shows every dependency, it needs no build change and it
can be switched off again in seconds. Use the starter for the services where the agent is not
an option. Both are configured through the same `OTEL_*` variables, so the rest of this page
applies to either.

## Step 2: Attach the Java agent

Pin a release of
[opentelemetry-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases)
(2.x; the real-data rounds used 2.15) and put the jar into the image; the JVM picks it up from
`JAVA_TOOL_OPTIONS`, so the launch command stays as it is.

```dockerfile
ADD --chmod=444 https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.15.0/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar
ENV JAVA_TOOL_OPTIONS="-javaagent:/otel/opentelemetry-javaagent.jar"
```

`--chmod=444` matters: a file added from a URL is readable by root only, and your service runs
as another user. Teams that share one base image or one Helm chart across services add the jar once, in the
shared Dockerfile, and switch it on per service with the variable. On Kubernetes the same jar
can come from an init container that copies it into an `emptyDir` volume, and the
OpenTelemetry Operator injects it with one annotation
(`instrumentation.opentelemetry.io/inject-java: "true"`) when you run the operator anyway.

Keep a kill switch: `OTEL_JAVAAGENT_ENABLED=false` turns the agent off without touching the
image, and `OTEL_SDK_DISABLED=true` does the same for the starter.

## Step 3: Configure the exporter

Every setting is an environment variable; the Spring Boot starter also reads them as
properties (`OTEL_SERVICE_NAME` is `otel.service.name`). Start from this set:

```text
OTEL_SERVICE_NAME=orders
OTEL_RESOURCE_ATTRIBUTES=service.namespace=sales,service.version=2.8.1
OTEL_EXPORTER_OTLP_PROTOCOL=grpc
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector.observability.svc:4317
OTEL_TRACES_EXPORTER=otlp
OTEL_METRICS_EXPORTER=none
OTEL_LOGS_EXPORTER=none
OTEL_TRACES_SAMPLER=parentbased_always_on
```

- `OTEL_EXPORTER_OTLP_PROTOCOL=grpc` matters: the SDKs default to HTTP/protobuf, which a gRPC
  port (`4317` on a collector, `4319` on the agent) does not accept. Send to the collector of
  the environment; it forwards to the Architrace agent next to your tracing backend
  ([installation guide, step 4](../deployment/#step-4-route-traces-to-the-agent)).
- `OTEL_METRICS_EXPORTER=none` and `OTEL_LOGS_EXPORTER=none` keep the SDK from exporting a
  second set of metrics or logs. Your existing Micrometer and Prometheus output stays
  byte-identical: metric families and sample names do not change when tracing is switched on,
  with the agent as with the starter.
- Sampling: keep a **parent-based** sampler. Architrace pairs the client span of a call with
  the server span of the same trace, and parent-based sampling keeps a trace whole, so every
  edge still appears when you sample. `OTEL_TRACES_SAMPLER=parentbased_traceidratio` with
  `OTEL_TRACES_SAMPLER_ARG=0.1` keeps one trace in ten; the latency percentiles then describe
  the sampled calls, the call counts count sampled calls.
- The Java agent instruments everything it finds; switch off what you do not need with
  `OTEL_INSTRUMENTATION_<NAME>_ENABLED=false` (for example `OTEL_INSTRUMENTATION_JDBC_DATASOURCE_ENABLED=false`).

For the Spring Boot starter, the dependency and the same settings as properties:

```kotlin
implementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.15.0"))
implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")
```

```yaml
otel:
  service:
    name: orders
  resource:
    attributes:
      service.namespace: sales
      service.version: ${APP_VERSION:unknown}
  exporter:
    otlp:
      protocol: grpc
      endpoint: http://otel-collector.observability.svc:4317
  traces:
    exporter: otlp
  metrics:
    exporter: none
  logs:
    exporter: none
```

Environment variables win over the properties file, so a platform team can set the exporter
once in the chart and leave the service name and version to the application. One warning for
starter users: the Kafka `TracingProducerInterceptor` and `TracingConsumerInterceptor` from the
instrumentation library do nothing with the starter, because the starter registers no global
OpenTelemetry instance for them to find; Kafka Streams and raw clients need the Java agent.

## Step 4: Send the attributes Architrace keys on

A span carries resource attributes about the service that produced it and span attributes
about the call. Architrace derives everything from them:

| Attribute | Architrace uses it for | Set it where |
|-----------|------------------------|--------------|
| `service.name` | the service node; spans without it are dropped | `OTEL_SERVICE_NAME` per service |
| `service.namespace` | the domain of the service; the cross-domain coupling rule counts domains | `OTEL_RESOURCE_ATTRIBUTES` per service, the team or the bounded context |
| `service.version` | the version shown on the map and compared by the Drift screen | `OTEL_RESOURCE_ATTRIBUTES` from the build (`${APP_VERSION}`) |
| `deployment.environment.name` | the environment; must equal the agent's `environment` or the span is dropped as foreign | the collector of the environment (`resource` processor), or the chart |
| `k8s.cluster.name` | the cluster of the scope | the collector, or the chart |
| `k8s.namespace.name`, `k8s.pod.name` | the deployment shown per service | the collector's `k8sattributes` processor, or the downward API |
| `server.address`, `server.port` | the peer of an HTTP client span: another service when the server span matches, an external host otherwise | the HTTP client instrumentation, automatically |
| `db.system`, `db.namespace` | the database node | JDBC and R2DBC instrumentation, automatically |
| `messaging.system`, `messaging.destination.name` | the topic node and the publish and consume edges | Kafka instrumentation, automatically |

The first six are yours to set; the rest come with the instrumentation. The
[configuration reference](../../reference/configuration/#attribute-mapping) lists every key the
agent tries for each field, legacy names included, and how to map your own.

Setting the scope attributes at the collector is the practice that scales: every service of
an environment passes through that environment's collector, so one `resource` processor stamps
`deployment.environment.name` and `k8s.cluster.name` on every span, and no service can report
itself into the wrong scope. On Kubernetes the `k8sattributes` processor adds the namespace and
the pod name in the same place:

```yaml
processors:
  k8sattributes:
    extract:
      metadata: [k8s.namespace.name, k8s.pod.name]
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
      processors: [k8sattributes, resource/scope]
      exporters: [otlp/architrace]
```

Without a collector, set them in the pod spec with the downward API:

```yaml
env:
  - name: POD_NAMESPACE
    valueFrom:
      fieldRef:
        fieldPath: metadata.namespace
  - name: POD_NAME
    valueFrom:
      fieldRef:
        fieldPath: metadata.name
  - name: OTEL_RESOURCE_ATTRIBUTES
    value: service.namespace=sales,service.version=2.8.1,deployment.environment.name=PROD,k8s.cluster.name=eu-1,k8s.namespace.name=$(POD_NAMESPACE),k8s.pod.name=$(POD_NAME)
```

## Step 5: Roll out

1. **One service first.** Attach the agent to one service in one environment, deploy, and open
   the map: the service, its database and its topics should appear within two snapshot
   intervals. Fix attributes here, where it is cheap.
2. **Watch the Architrace agent's counters** (`/metrics` on `9464`):
   `architrace_agent_spans_received_total` grows, `architrace_agent_spans_foreign_total` stays
   at zero (otherwise the environment attribute disagrees with the agent's), and
   `architrace_agent_spans_rejected_total` stays at zero (otherwise raise `buffers.ring-size`).
3. **Then the rest of the environment**, team by team. Edges between two services need both
   sides instrumented; until then a call shows as an edge to an external host named after the
   server address, which is the honest picture of what the traces say.
4. **Then the other environments**, each with its own agent and the same variables, so the
   Drift screen can compare them.
5. **Pin and record.** Pin the Java agent version in the image, record the `OTEL_*` set in the
   chart, keep the kill switch documented, and move the agent version with your normal
   dependency cadence.

## Other languages

Any OpenTelemetry SDK or auto-instrumentation works the same way; only the mechanism differs.
Python: `opentelemetry-instrument` with the same `OTEL_*` variables (the Python requests
instrumentation needs `OTEL_SEMCONV_STABILITY_OPT_IN=http` for the current attribute names, as
the [demo](../docker-demo/) shows). Node.js: `--require @opentelemetry/auto-instrumentations-node/register`.
Go and Rust need the SDK in code. The attributes of step 4 are the contract; the language is
not.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| The service is missing from the map | no spans arrive, or `service.name` is missing | check the exporter protocol (`grpc`) and endpoint; check the collector pipeline includes the Architrace exporter; a span without `service.name` is dropped |
| The scope never appears, `spans_foreign` grows | `deployment.environment.name` differs from the agent's `environment` | stamp the environment at the collector, or fix the variable; the comparison is exact |
| The database is missing | no JDBC or R2DBC span | the starter covers `DataSource` based access; a driver used directly needs the Java agent |
| A topic is missing | Kafka Streams, reactor-kafka or a raw client | attach the Java agent; the starter and the interceptors do not see them |
| An internal `-changelog` or `-repartition` topic shows up | Kafka Streams internals | they are ignored by default; extend `topics.ignore` in the agent file for your own patterns |
| A call shows as an external host instead of a service | the callee is not instrumented, or context propagation is broken | instrument the callee; check that the HTTP client propagates `traceparent` (the Java agent does) |
| An external host appears only after two minutes | the client span waits for a partner that never comes | lower `buffers.pending-ttl-seconds` on the agent |
| Latencies look wrong after sampling | percentiles describe the sampled calls | keep the parent-based sampler; lower the ratio only where the volume demands it |
| Every service calls the same host and the external lens is noise | a feature-flag or configuration server | list it in `ARCHITRACE_TOPOLOGY_PLATFORMHOSTS` on the control plane |
