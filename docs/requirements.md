# Architrace requirements

Status: **draft for maintainer review**. This document records what the project is for, what
exists today, what is missing, and a proposed MVP scope. Once the MVP scope is agreed the
"Agreed MVP" section becomes the contract for the design stage.

Related: `docs/progress.md`, `docs/github-access.md`, `AGENTS.md`.

---

## 1. Vision

Architrace turns runtime telemetry into **architecture intelligence**. An architect should be
able to open one tool and see, per environment, the real services, data streams and
dependencies as they exist right now, how they differ from another environment or from the
previous release, and what an AI-assisted analysis thinks about it.

Target user: a software / solution architect responsible for a landscape of 10–300 services
instrumented with OpenTelemetry, typically deployed on Kubernetes across DEV / TEST / STAGE /
PROD.

Non-goals for the foreseeable future: replacing an APM or tracing backend (Jaeger, Tempo),
storing raw spans, alerting on SLOs, enterprise architecture repositories (ArchiMate).

## 2. Product capabilities (full picture)

Numbered so that MVP decisions can refer to them.

| ID  | Capability | Description |
|-----|------------|-------------|
| C1  | Telemetry ingestion | Receive OTLP traces (gRPC, later HTTP) from collectors or SDKs. |
| C2  | Graph extraction | Derive nodes (service, database, message topic / queue, external HTTP service) and edges (sync call, async publish / consume) from spans, with per-edge metrics (calls, errors, latency p50 / p95 / p99). |
| C3  | Environment and deployment context | Attach environment, domain, cluster, namespace, service version to every node using standard OpenTelemetry resource attributes with configurable fallbacks. |
| C4  | Agent → control plane streaming | Periodic graph snapshots per agent over bidirectional gRPC; agent registration, health, config push. |
| C5  | Topology storage | Persist snapshots and a current "live" graph per environment with history. |
| C6  | Query API | REST + OpenAPI: environments, domains, services, dependencies, snapshots, diffs, findings. |
| C7  | Visualisation | Web UI: service map per environment, filter by domain / node type, node and edge details, time selector. |
| C8  | Environment drift | Diff two environments: services and dependencies present in one and not the other, version differences. |
| C9  | Release drift | Diff two snapshots of the same environment (before / after a release): added, removed, changed dependencies; version changes. |
| C10 | Data streams view | Topics / queues as first-class nodes with producers and consumers, fan-in / fan-out, orphan topics. |
| C11 | Architecture rules | Deterministic checks: cyclic dependencies, cross-domain coupling, single points of failure, synchronous chains over N hops, direct DB sharing, unknown external calls. |
| C12 | AI insights | LLM-assisted narrative: explain the graph, summarise drift, propose improvements; provider-agnostic, optional. |
| C13 | Declared vs actual | Import an intended architecture model and report violations. |
| C14 | Exports | Mermaid / C4-style diagrams, JSON, event catalogue. |
| C15 | Agent management | Remote config (sampling, attribute mapping), health dashboard for agents. |
| C16 | Multi-tenancy and auth | Users, roles, API tokens. |
| C17 | Packaging | Docker images, docker compose demo, Helm chart, native image. |

## 3. What exists today (inventory of `main`, 2026-10-01)

### 3.1 Repository and build

- Gradle 9.3.1, Kotlin DSL, Java 25 toolchain with `--enable-preview`, version catalogue in
  `gradle/libs.versions.toml`. Modules: `api`, `agent`, `control-plane`.
- Root plugins: JaCoCo (85% line / branch / method verification), Spotless (SPDX license header
  only, **no formatter**), SonarQube, GraalVM native, Shadow.
- Hard-coded version `0.1.0` in `gradle.properties` and in the CLI `@Command(version = ...)`.
- `gradlew` is not executable in git; the pipelines run `chmod +x`.

### 3.2 `architrace-api`

- `architrace-agent.proto`: `ControlPlaneService.Connect` (bidi stream) and `GetAgentHealth`.
  Messages: `AgentRegister`, `GraphBatch{nodes, edges(call_count)}`, `ConfigUpdate`,
  health request / response.
- `openapi.yaml` is empty.

### 3.3 `architrace-agent` (≈2.6k lines main, ≈1.4k lines test)

Works:

- picocli CLI with Guice (`run`, `dry-run`, `version`). YAML config loading and validation
  (`clusterId`, `agent.name`, `control-plane.bootstrap.server`, optional port / retry).
- OTLP trace receiver (gRPC server with health service) → `SpanExtractor` →
  `InternalSpan` records → lock-free `SpanRingBuffer` → `SpanBatchProcessor` →
  `SpanPipeline` of `SpanProcessor`s.
- `NodeRegistry` + `NodeExtractor` classify nodes (service / database / topic / external).
- `SyncDependencyResolver` pairs CLIENT → SERVER spans (with out-of-order buffering),
  `AsyncDependencyResolver` pairs PRODUCER → CONSUMER by trace + destination.
- Control-plane client: channel, bidi stream, inbound / outbound dispatchers with overflow
  strategy, registration, reconnect supervisor, `StructuredTaskScope` runtime.

Gaps and defects found:

| # | Finding | Impact |
|---|---------|--------|
| A1 | `ControlPlaneClient.send(GraphSnapshot)` builds an **empty** event; snapshot → `GraphBatch` mapping is commented out. | No topology ever reaches the control plane. |
| A2 | `AgentRuntimeService.run` evaluates `activeLifecycle.getTransportClient()` inside a forked task before the lifecycle is bootstrapped. | Likely `NullPointerException` at startup; end-to-end flow unverified. |
| A3 | `InternalSpan` has no start / end timestamps, status or `service.version`. `EdgeMetrics.record` is never called. | No latency, error rate or version data. |
| A4 | Attribute mapping uses custom resource keys (`environment`, `domainId`, `serviceName`, `cluster`, `namespace`) and deprecated span keys (`http.host`, `http.method`, `messaging.destination`, `db.name`). | Standard OTel instrumentation (Spring Boot starter, Java agent) produces no usable graph. |
| A5 | Two `GraphNode` types (`model.GraphNode` interface vs `otlp.GraphNode` class), `GraphAggregator` vs `GraphSnapshotService`, dead `NodeAggregator`, empty `SamplingProcessor` / `AnomalyDetectionProcessor`. | Duplicated / dead code. |
| A6 | `ControlPlaneServiceImpl` + `ControlPlaneRegistry` (a server) live inside the agent module. | Wrong module boundary; duplicates the control-plane service. |
| A7 | Snapshot interval (60 s), ring buffer size, queue capacity are hard-coded. | Not configurable. |
| A8 | `dry-run` only logs; `--prop` overrides are not applied. | Command is a stub. |
| A9 | Agent test sources **do not compile** (`ThrowingTransportClient` misses `send`). Tests use reflection to call private methods. | Agent suite cannot run; coverage unknown. |
| A9b | `api` has no tests; `control-plane` has 3 tests with 50% branch and 82% method coverage. | The 85% JaCoCo gate fails on `control-plane` and is vacuous on `api`. |
| A10 | Spotless check fails (`GraphAggregator` lacks the license header, `SpanReceiver` has an extra blank line). | Every pipeline is red. |
| A11 | `AsyncDependencyResolver` keeps producers forever when the consumer never arrives; `GlobalSpanRegistry` and `waitingServers` grow without eviction. | Memory leak under load. |
| A12 | `SpanRingBuffer.publish` returns `false` when full and the caller ignores it. | Silent data loss without a metric. |

### 3.4 `architrace-control-plane` (≈180 lines)

- Spring Boot 4.0.3 (milestone / snapshot repositories enabled), Spring gRPC 1.0.2, Actuator,
  springdoc, PostgreSQL driver on the classpath but **no datasource, no persistence, no REST
  controllers**.
- `AgentService.connect` replies with a hard-coded `ConfigUpdate` on registration and ignores
  graph batches; `getAgentHealth` always answers `live=true`.
- `GrpcConfig` is empty. The build file still says "Demo project for Spring Boot".

### 3.5 Demo, docs, packaging

- `otel-test-app`: three Flask services with OTel SDK, collector forwarding to the agent on
  `4319`. Agent and control plane are commented out in `docker-compose.yml`. `__pycache__`
  is committed.
- Root `Dockerfile` and `Dockerfile-fast` reference modules that do not exist
  (`agent-api`, `agent-core`, `cli`, `exporter-*`, `diff`); `Dockerfile.native` reads the jar
  from `agent/build/libs` (real path `architrace-agent/build/libs`). None of them builds.
- Documentation site (Astro Starlight) deploys to GitHub Pages from `main` and is partly out
  of date versus the code (config keys, required fields).

### 3.6 CI/CD and repository process

- Three overlapping PR workflows: `agent.yml` ("PR Checks"), `pr-ci.yml` ("PR CI", **invalid**:
  `secrets` in step-level `if`, fails in 0 s) and the main `ci-cd.yml` ("Merge CI/CD", fails
  at Spotless). `docs-deploy.yml` works.
- `ci-cd.yml` builds a GraalVM native image via the broken `Dockerfile.native`, pushes to GHCR,
  and publishes a GitHub release on `v*` tags. No tag has ever been created.
- SonarCloud and Snyk are integrated by token; no OWASP Dependency-Check, CodeQL, Dependabot,
  secret scanning or container scanning.
- No version automation, no changelog, no CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, CODEOWNERS,
  `.editorconfig`, `dependabot.yml`.
- `.github/ PULL_REQUEST_TEMPLATE.md` and `.github/ ISSUE_TEMPLATE/` have a **leading space**
  in their names, so GitHub ignores them; `ISSUE_TEMPLATE/` is duplicated at the repository
  root; `.github/profile/README.md` belongs to the organisation `.github` repository.
- Branch ruleset exists but is disabled; no required checks; all 19 PRs were merged without
  gates.

## 4. Requirements

### 4.1 Functional (product)

| ID | Requirement | Capability |
|----|-------------|------------|
| F1 | Accept OTLP/gRPC traces from an OpenTelemetry Collector and directly from SDKs. | C1 |
| F2 | Resolve service identity and context from standard OTel resource attributes (`service.name`, `service.namespace`, `service.version`, `deployment.environment.name`, `k8s.cluster.name`, `k8s.namespace.name`) with a configurable mapping for custom attributes. | C3 |
| F3 | Support both current and legacy HTTP / DB / messaging semantic conventions when classifying spans. | C2 |
| F4 | Build nodes of type service, database, topic / queue, external service; edges of type sync and async; per-edge calls, errors, latency distribution; per-node version(s) and deployments. | C2, C10 |
| F5 | Publish complete graph snapshots to the control plane on a configurable interval; register and heartbeat; survive control-plane restarts. | C4 |
| F6 | Persist snapshots and maintain the current graph per environment; retain history for a configurable period. | C5 |
| F7 | Expose a REST API with OpenAPI for environments, services, graph, snapshots, diffs and findings. | C6 |
| F8 | Provide a web UI with an interactive service map per environment, filters, node / edge details and a snapshot time selector. | C7 |
| F9 | Compute and display environment drift (A vs B) and release drift (snapshot vs snapshot). | C8, C9 |
| F10 | Run deterministic architecture rules on each snapshot and show findings with severity. | C11 |
| F11 | Offer AI insights on demand through a pluggable LLM provider; the product works fully without it. | C12 |
| F12 | Ship Docker images for agent and control plane and a one-command demo. | C17 |

### 4.2 Non-functional

| ID | Requirement |
|----|-------------|
| N1 | Agent: sustain 10k spans/s on 2 vCPU with bounded memory (eviction for unmatched spans, metrics for drops). |
| N2 | Agent never blocks the OTLP caller; back-pressure is observable through metrics. |
| N3 | Control plane: stateless app + PostgreSQL; horizontal scale not required for MVP. |
| N4 | Everything observable: Micrometer / Actuator metrics and health for both components. |
| N5 | Secure by default: TLS-ready gRPC, no secrets in config files, scanners in CI. |
| N6 | Java 25 idioms: records, sealed types, pattern matching, virtual threads, structured concurrency; DOP style with behaviour outside data. |
| N7 | 85%+ test coverage kept; integration tests with Testcontainers for PostgreSQL and gRPC. |
| N8 | Apache-2.0, public, no references to private infrastructure. |

### 4.3 Engineering process

| ID | Requirement |
|----|-------------|
| P1 | PR-only development, squash merge, Conventional Commits with ticket scope, PR template filled for every PR. |
| P2 | Automated semantic versioning from commit history (see §5.3); Git tag + GitHub release + images on merge to `main`. |
| P3 | PR pipeline: format, compile, unit + integration tests, coverage gate, static analysis, security scans, AI code review. |
| P4 | Main pipeline: everything above plus build, version, tag, publish images and docs. |
| P5 | ADRs for every architectural decision, written only after agreement. |
| P6 | Living docs: requirements, progress, architecture, feature docs, README, docs site. |
| P7 | No documentation comments in code. |

## 5. Proposed engineering platform

### 5.1 Repository hygiene (first feature after agreement)

Fix Spotless and test compilation on `main`; remove or fix the three broken Dockerfiles;
rename the space-prefixed GitHub templates; remove duplicated `ISSUE_TEMPLATE`, committed
`__pycache__`, the org-profile README; add `.editorconfig`, `CONTRIBUTING.md`,
`CODE_OF_CONDUCT.md`, `SECURITY.md`, `CODEOWNERS`, `dependabot.yml`; make `gradlew`
executable; create the labels from `LABELS.md`.

### 5.2 CI/CD flow (GitHub Actions)

One PR workflow and one main workflow, modelled on a staged pipeline:

```
PR:    init → compile → [format | unit tests + coverage | static analysis | security] → ai-review → status
main:  init → compile → tests → static analysis → security → build artifacts → version + tag
       → images (GHCR) → GitHub release → docs deploy
```

- Reusable composite action for JDK + Gradle setup; Gradle build cache; all actions pinned by SHA.
- Required checks on `main`: `build`, `quality`, `security`, `ai-review`.
- Release channel: every merge to `main` produces `X.Y.Z-<sha>` artifacts; a release tag
  `vX.Y.Z` is created automatically when the computed version changes (or manually via
  `workflow_dispatch`, to be decided).

### 5.3 Versioning

Port the proven approach into this repository as an included build (`build-logic/`): the
`axion-release` plugin reads the last `v*` tag, scans Conventional Commits since that tag,
decides major / minor / patch, produces `X.Y.Z-<shortSha>-SNAPSHOT` between releases and a
clean `X.Y.Z` on the tag. Tasks: `printVersion`, `printReleaseVersion`, `releaseNotes`
(Markdown grouped by commit type, feeding the GitHub release body). The CLI `version`
command reads the version from the manifest instead of a literal.

### 5.4 AI code review (second agent in the PR pipeline)

| Option | How it runs | Cost | Notes |
|--------|-------------|------|-------|
| **CodeRabbit** (recommended) | GitHub App; reviews every PR, line comments, summary, learns from `.coderabbit.yaml`; can "request changes" and block merge until threads are resolved. | Free for public repositories. | Highest review quality in independent comparisons; zero infrastructure; maintainer installs the app. |
| Qodo PR-Agent | Open-source GitHub Action step (`/review`, `/improve`, `/describe`); bring-your-own LLM key (OpenAI, Gemini, Anthropic). | LLM API usage. | Literally "a step in the workflow"; fully under repo control. |
| Gemini Code Assist | GitHub App by Google. | Free for public repositories. | Good summaries; weaker inline findings than CodeRabbit in comparisons. |
| GitHub Copilot code review | Native, ruleset-triggered (`copilot_code_review`, already in the disabled ruleset). | Requires a paid Copilot plan even on public repos. | Not free. |

Proposal: CodeRabbit as the blocking reviewer plus `required conversation resolution` in the
ruleset. If the maintainer prefers an in-workflow step, PR-Agent with a Gemini key.

### 5.5 Static analysis and security

| Tool | Role | Status |
|------|------|--------|
| Spotless + palantir-java-format | formatting, license header | header only today |
| Checkstyle (Google-based, adjusted) | style rules | missing |
| SpotBugs + find-sec-bugs | bug patterns, security patterns | missing; Java 25 support to be verified in design |
| SonarCloud | quality gate on new code | configured, project key to confirm |
| JaCoCo | coverage gate 85% | configured |
| Snyk | dependency and container vulnerabilities (OWASP Top 10 coverage via SCA) | token present |
| OWASP Dependency-Check | NVD-based SCA, SARIF to GitHub Security | missing |
| CodeQL | SAST for Java, free for public repos | missing |
| Trivy | image scan before push to GHCR | missing |
| Gitleaks + GitHub secret scanning | secrets | missing |
| Dependabot | dependency updates (Gradle, Actions, npm, pip) | missing |
| OpenSSF Scorecard | OSS best-practice badge | optional |

### 5.6 Documentation layout

```
AGENTS.md                      agent / contributor operating rules
README.md                      user-facing overview
docs/requirements.md           this file
docs/progress.md               stage and feature tracking
docs/architecture.md           current architecture, updated after each implementation PR
docs/adr/NNNN-title.md         decisions (after agreement)
docs/features/<name>.md        one document per feature
documentation/…                published docs site (user guides, reference)
```

## 6. Proposed MVP scope

Guiding question: what is the smallest product the maintainer can deploy at work and get
architecture value from within a day?

### In scope

| # | Feature | Covers | Why |
|---|---------|--------|-----|
| M1 | **Agent pipeline completion**: timestamps / status / version in spans, standard OTel attribute mapping (F2, F3), edge metrics, eviction and drop metrics, configurable intervals, real `GraphBatch` publishing, module boundary cleanup (A1–A12). | F1–F5, N1, N2 | Nothing works end-to-end without it. |
| M2 | **Control plane ingestion and storage**: PostgreSQL schema, snapshot persistence, current graph per environment, retention. | F6, N3 | Foundation for every view. |
| M3 | **Query API** with OpenAPI. | F7 | Needed by UI and by scripts. |
| M4 | **Service map UI**: per-environment graph, filters, details, time selector. | F8 | The visible value. |
| M5 | **Drift**: environment diff and release diff, API + UI. | F9 | The differentiator the maintainer asked for. |
| M6 | **Architecture rules** (deterministic set, 5–6 rules) with findings in API and UI. | F10 | Immediate architecture feedback without an LLM. |
| M7 | **Packaging and demo**: images for agent and control plane, docker compose with collector + demo services + PostgreSQL, docs. | F12 | Deployable at work. |
| M0 | **Engineering platform**: §5.1–5.5. | P1–P7 | Required before feature work. |

### Out of MVP (next)

- C12 AI insights (LLM provider abstraction and prompts) — designed in MVP so the API has a
  place for it, implemented after.
- C13 declared-vs-actual, C14 exports, C15 remote agent config beyond what exists,
  C16 auth, Helm chart, native image (preview-feature risk with GraalVM).

### Open questions for the maintainer

1. Agree the "in scope" list, or move items (for example, M6 out, or C12 in).
2. UI stack preference: React + TypeScript SPA served by the control plane (recommended for an
   interactive graph), or server-side rendering kept inside Spring Boot.
3. Storage: PostgreSQL agreed? Migration tool: Flyway (recommended) or Liquibase.
4. AI reviewer choice (§5.4) and review identity option (`docs/github-access.md` §4).
5. Ticket numbering: where do `ARCHI-<n>` numbers come from, and the next free number.
6. Is `environment` a required agent config value (one agent per environment) or derived per
   span? Recommendation: required in config, overridable by resource attribute.
7. Keep Spring Boot 4 milestone / snapshot repositories, or pin to a GA line for stability.

## 7. Agreed MVP

_To be filled after the maintainer's decision on §6._
