<div align="center">
  <h1>Architrace</h1>

  <p align="center">
    <strong>Runtime architecture intelligence for distributed systems</strong>
    <br/>
    Collect OTLP traces, build service graphs, and stream topology to a control plane
    <br/><br/>
  </p>

  ![Java](https://img.shields.io/badge/Java-25-blue)
  ![Gradle](https://img.shields.io/badge/Gradle-9.3.1-02303A?logo=gradle)
  ![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)

  [Documentation](https://architrace-intelligence.github.io/Architrace/) | [Quick Start](#quick-start) | [Docker Demo](#docker-demo) | [Contributing](#contributing)
</div>

---

## Quick Start

Run quality gates + tests:

```bash
./gradlew spotlessCheck classes test jacocoTestReport
```

Build all modules:

```bash
./gradlew build
```

Run control-plane locally:

```bash
./gradlew :control-plane:bootRun
```

Build runnable agent fat jar:

```bash
./gradlew :agent:shadowJar
```

---

## Features

- **OTLP Ingestion** - Receives traces on OTLP gRPC (`:4319`)
- **Graph Transformation** - Converts spans into nodes/edges and graph batches
- **Control Plane Stream** - Bidirectional gRPC session between agent and control-plane
- **Query API** - REST under `/api/v1` from an OpenAPI 3.1 contract, with Swagger UI at `/swagger-ui`
- **Structured Concurrency** - Runtime built on Java 25 concurrency primitives
- **Modular Monorepo** - Separate modules for runtime agent, control-plane, and shared API contracts

---

## Monorepo Structure

- **[`architrace-agent`](./architrace-agent)** - Runtime agent CLI, OTLP receiver, graph pipeline
- **[`architrace-control-plane`](./architrace-control-plane)** - Spring Boot service (HTTP + gRPC)
- **[`architrace-api`](./architrace-api)** - Shared protobuf and OpenAPI contracts and generated classes
- **[`otel-test-app`](./otel-test-app)** - End-to-end demo stack (Python services + collector + Architrace)

---

## Architecture

```mermaid
flowchart LR
  OTel[OTel SDK / Collector] -->|OTLP gRPC :4319| Agent[Architrace Agent]
  Agent -->|Bidirectional gRPC :9090| CP[Control Plane]
  CP -->|HTTP :8085| Ops[Operators / APIs]
  Agent --> API[Shared API]
  CP --> API
```

---

## Docker Demo

Run full local demo stack:

```bash
cd otel-test-app
docker compose build
docker compose up -d
```

Traffic generator endpoint:

```bash
curl http://localhost:8080/
```

Expected response:

```text
A -> B -> C
```

---

## Common Commands

Format all modules:

```bash
./gradlew spotlessApply
```

Run only agent tests:

```bash
./gradlew :agent:test
```

Run only control-plane tests:

```bash
./gradlew :control-plane:test
```

Generate protobuf classes:

```bash
./gradlew :agent:generateProto :control-plane:generateProto :api:generateProto
```

---

## CI

Pull requests run [`PR Checks`](./.github/workflows/agent.yml): Spotless, compilation, tests with
JaCoCo coverage, SonarCloud analysis and a jar build. Merges to `main` run
[`Merge CI/CD`](./.github/workflows/ci-cd.yml), which adds Snyk monitoring and publishes release
artifacts on `v*` tags. Both pipelines are being replaced by the gated flow described in the
[M0 design](https://architrace-intelligence.github.io/Architrace/project/features/m0-engineering-platform/).

---

## Contributing

Every change goes through a pull request with automated checks and review. Read
[CONTRIBUTING.md](./CONTRIBUTING.md) and the
[documentation site](https://architrace-intelligence.github.io/Architrace/) for the process,
the agreed requirements and the current state of the work.

---

## License

Apache-2.0. See [LICENSE](./LICENSE).
