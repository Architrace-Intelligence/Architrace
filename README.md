<div align="center">
  <h1>Architrace</h1>

  <p align="center">
    <strong>Runtime architecture intelligence for distributed systems</strong>
    <br/>
    Collect OTLP traces, build service graphs, and stream topology to a control plane
    <br/><br/>
  </p>

  [![Main pipeline](https://github.com/Architrace-Intelligence/Architrace/actions/workflows/main.yml/badge.svg)](https://github.com/Architrace-Intelligence/Architrace/actions/workflows/main.yml)
  [![CodeQL](https://github.com/Architrace-Intelligence/Architrace/actions/workflows/codeql.yml/badge.svg)](https://github.com/Architrace-Intelligence/Architrace/actions/workflows/codeql.yml)
  [![Tests](https://img.shields.io/endpoint?url=https%3A%2F%2Farchitrace-intelligence.github.io%2FArchitrace%2Fbadges%2Ftests.json)](https://github.com/Architrace-Intelligence/Architrace/actions/workflows/main.yml)
  [![Quality gate](https://sonarcloud.io/api/project_badges/measure?project=Architrace-Intelligence_Architrace-agent&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Architrace-Intelligence_Architrace-agent)
  [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Architrace-Intelligence_Architrace-agent&metric=coverage)](https://sonarcloud.io/component_measures?id=Architrace-Intelligence_Architrace-agent&metric=coverage)
  [![Snyk](https://snyk.io/test/github/Architrace-Intelligence/Architrace/badge.svg)](https://snyk.io/test/github/Architrace-Intelligence/Architrace)

  [![Release](https://img.shields.io/github/v/release/Architrace-Intelligence/Architrace?sort=semver&display_name=tag&label=release)](https://github.com/Architrace-Intelligence/Architrace/releases)
  [![Version on main](https://img.shields.io/endpoint?url=https%3A%2F%2Farchitrace-intelligence.github.io%2FArchitrace%2Fbadges%2Fversion.json)](https://github.com/Architrace-Intelligence/Architrace/commits/main)
  [![Images](https://img.shields.io/badge/ghcr.io-agent%20%7C%20control--plane-2496ED?logo=docker&logoColor=white)](https://github.com/orgs/Architrace-Intelligence/packages?repo_name=Architrace)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)

  [![Java](https://img.shields.io/badge/dynamic/regex?url=https://raw.githubusercontent.com/Architrace-Intelligence/Architrace/main/gradle.properties&search=javaVersion%3D%28%5B0-9%5D%2B%29&replace=%241&label=Java&logo=openjdk&color=blue)](./gradle.properties)
  [![Gradle](https://img.shields.io/badge/dynamic/regex?url=https://raw.githubusercontent.com/Architrace-Intelligence/Architrace/main/gradle/wrapper/gradle-wrapper.properties&search=gradle-%28%5B0-9.%5D%2B%29-bin&replace=%241&label=Gradle&logo=gradle&color=02303A)](./gradle/wrapper/gradle-wrapper.properties)
  [![Spring Boot](https://img.shields.io/badge/dynamic/toml?url=https://raw.githubusercontent.com/Architrace-Intelligence/Architrace/main/gradle/libs.versions.toml&query=%24.versions.spring-boot&label=Spring%20Boot&logo=springboot&color=6DB33F)](./gradle/libs.versions.toml)
  [![Node.js](https://img.shields.io/badge/dynamic/regex?url=https://raw.githubusercontent.com/Architrace-Intelligence/Architrace/main/gradle.properties&search=nodeVersion%3D%28%5B0-9.%5D%2B%29&replace=%241&label=Node.js&logo=nodedotjs&color=5FA04E)](./gradle.properties)
  [![React](https://img.shields.io/github/package-json/dependency-version/Architrace-Intelligence/Architrace/react?filename=architrace-ui%2Fpackage.json&label=React&logo=react&color=20232A)](./architrace-ui/package.json)
  [![TypeScript](https://img.shields.io/github/package-json/dependency-version/Architrace-Intelligence/Architrace/dev/typescript?filename=architrace-ui%2Fpackage.json&label=TypeScript&logo=typescript&color=3178C6)](./architrace-ui/package.json)
  [![Vite](https://img.shields.io/github/package-json/dependency-version/Architrace-Intelligence/Architrace/dev/vite?filename=architrace-ui%2Fpackage.json&label=Vite&logo=vite&color=646CFF)](./architrace-ui/package.json)

  [Documentation](https://architrace-intelligence.github.io/Architrace/) | [Quick Start](#quick-start) | [Docker Demo](#docker-demo) | [Contributing](#contributing)
</div>

---

## Quick Start

Run the quality gates (formatting, Checkstyle, tests with coverage, UI gate):

```bash
./gradlew spotlessApply check
```

Build all modules:

```bash
./gradlew build
```

Run control-plane locally:

```bash
./gradlew :architrace-control-plane:bootRun
```

Build runnable agent fat jar, validate a configuration, run the agent:

```bash
./gradlew :architrace-agent:shadowJar
java --enable-preview -jar architrace-agent/build/libs/architrace-agent-*-all.jar dry-run --config ./demo/agent-dev.yaml
java --enable-preview -jar architrace-agent/build/libs/architrace-agent-*-all.jar run --config ./demo/agent-dev.yaml --prop control-plane.server=localhost:9090
```

Print the version the build computes from git tags and Conventional Commits:

```bash
./gradlew printVersion printReleaseVersion
```

---

## Features

- **OTLP Ingestion** - Receives traces on OTLP gRPC (`:4319`) from collectors and SDKs; service identity, deployment and peers come from the standard OpenTelemetry semantic conventions (current and legacy keys), with a configurable attribute mapping
- **Graph Transformation** - Converts spans into nodes/edges and graph batches
- **Control Plane Stream** - Bidirectional gRPC session between agent and control-plane with registration, heartbeats, acknowledged snapshots and automatic reconnection
- **Agent Metrics** - Prometheus `/metrics` and `/health` on port `9464`, rate-limited reporting of rejected and evicted spans
- **Architecture rules** - eight deterministic rules (cyclic dependency, shared database, wide blast radius, cross-domain coupling, fan-in hub, long synchronous chain, unknown external, data stream without producer) evaluated after every snapshot with the evidence attached, and the blast radius of any node computed on request
- **Query API** - REST under `/api/v1` from an OpenAPI 3.1 contract: scopes, agents, the graph and services of a scope at a point in time, snapshot history, the drift of a scope against another scope of its project or against its own past, its findings and the impact of one of its nodes; Swagger UI at `/swagger-ui`
- **Web UI** - React + TypeScript single-page application served by the control plane at `/`, talking to the Query API through a client typed from the same contract: a Projects list filtered by environment and cluster, the Service map of a scope (React Flow with an ELK layered layout, finding badges and an impact lens), the Drift screen that compares a scope with another environment or with its own past, as a grouped list and as map overlays, and the Findings screen with the rules, their evidence and the rail
- **Structured Concurrency** - Runtime built on Java 25 concurrency primitives
- **Modular Monorepo** - Separate modules for runtime agent, control-plane, shared API contracts and the UI, with shared build conventions in `build-logic`
- **Versioned from git** - the version is computed from the last `v*` tag and the Conventional Commits since it; the CLI reports it from the jar manifest

---

## Monorepo Structure

- **[`architrace-agent`](./architrace-agent)** - Runtime agent CLI, OTLP receiver, graph pipeline
- **[`architrace-control-plane`](./architrace-control-plane)** - Spring Boot service (HTTP + gRPC)
- **[`architrace-api`](./architrace-api)** - Shared protobuf and OpenAPI contracts and generated classes
- **[`architrace-ui`](./architrace-ui)** - Web UI (Vite, React, TypeScript), built by Gradle and bundled into the control plane jar
- **[`build-logic`](./build-logic)** - Gradle convention plugins: Java toolchain and quality gates, Spring Boot defaults, versioning from git
- **[`demo`](./demo)** - One-command demo: two environments of a small shop, a broker, a database and an external host on the published images

---

## Architecture

```mermaid
flowchart LR
  OTel[OTel SDK / Collector] -->|OTLP gRPC :4319| Agent[Architrace Agent]
  Agent -->|Bidirectional gRPC :9090| CP[Control Plane]
  CP -->|HTTP :8085| UI[Web UI / Query API clients]
  Agent --> API[Shared API]
  CP --> API
```

---

## Docker Demo

Run the full stack with two environments, a drift between them and findings out of the box:

```bash
cd demo
docker compose up -d
```

Open <http://localhost:8085>: the Projects list shows the project `demo` with the scopes `DEV`
and `STAGE` within a minute; the map, the Drift screen and the Findings screen have something
to show from the first snapshot on. What runs and what to click first is in the
[Docker demo guide](https://architrace-intelligence.github.io/Architrace/guides/docker-demo/).

```bash
docker compose down -v
```

## Common Commands

Format all modules:

```bash
./gradlew spotlessApply
```

Run only agent tests:

```bash
./gradlew :architrace-agent:test
```

Run only control-plane tests:

```bash
./gradlew :architrace-control-plane:test
```

Generate protobuf classes:

```bash
./gradlew :architrace-agent:generateProto :architrace-control-plane:generateProto :architrace-api:generateProto
```

Run the UI quality gate (types, lint, formatting, tests) or build its bundle; Gradle downloads the
pinned Node.js on first use:

```bash
./gradlew :architrace-ui:test
./gradlew :architrace-ui:npmBuild
```

Develop the UI against a running control plane (Node.js 22.12+, requests to `/api` are proxied
to `localhost:8085`):

```bash
cd architrace-ui
npm ci
npm run dev
```

---

## CI

Pull requests run [`PR`](./.github/workflows/pr.yml): `build` (`./gradlew build`: Spotless,
Checkstyle, tests with the JaCoCo coverage gate, the UI gate, jars), `quality` (SonarCloud with
the quality gate on new code), `security` (Snyk, OWASP Dependency-Check, gitleaks) and `docs`
(site build). [`PR title`](./.github/workflows/pr-title.yml) checks the title convention and
[`CodeQL`](./.github/workflows/codeql.yml) analyses the Java and TypeScript sources and the
workflows. Merges to `main` run [`Main`](./.github/workflows/main.yml): the same gates, then a
release when the commits since the last tag contain a releasing type (tag, notes, jars), the
container images, the documentation site and the dependency graph. The `main` ruleset
([`.github/rulesets/main.json`](./.github/rulesets/main.json)) makes those checks, resolved review
threads and a squash merge mandatory, and CodeRabbit ([`.coderabbit.yaml`](./.coderabbit.yaml))
reviews every pull request. Details on the
[M0 page](https://architrace-intelligence.github.io/Architrace/project/features/m0-engineering-platform/).

## Container images

Every merge to `main` publishes `ghcr.io/architrace-intelligence/architrace-agent` and
`ghcr.io/architrace-intelligence/architrace-control-plane` tagged `sha-<short>`; releases add
`X.Y.Z` and `latest`. The images run as a non-root user on Eclipse Temurin 25 JRE.

```bash
docker run --rm -p 8085:8085 -p 9090:9090 \
  -e ARCHITRACE_DB_URL=jdbc:postgresql://db:5432/architrace \
  ghcr.io/architrace-intelligence/architrace-control-plane:latest

docker run --rm -p 4317:4317 -v $PWD/architrace-agent.yaml:/config/architrace-agent.yaml \
  ghcr.io/architrace-intelligence/architrace-agent:latest
```

To build an image locally, produce a fresh jar first (`./gradlew clean assemble`), then
`docker build -t architrace-agent architrace-agent` or the same for `architrace-control-plane`.

---

## Contributing

Every change goes through a pull request with automated checks and review. Read
[CONTRIBUTING.md](./CONTRIBUTING.md) and the
[documentation site](https://architrace-intelligence.github.io/Architrace/) for the process,
the agreed requirements and the current state of the work.

---

## License

Apache-2.0. See [LICENSE](./LICENSE).
