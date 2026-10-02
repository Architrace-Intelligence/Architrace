---
title: M0. Engineering platform
description: Green gates on every PR, automatic versioning and publishing on main.
---

Status: in progress (PR 3 of 5 in review, ARCHI-34) · Order: 1 · Requirements: P1–P7, §5.1–5.5

## Goal

Every pull request is checked the same way, nothing reaches `main` without passing, and every
merge to `main` produces versioned artifacts without manual steps.

## Scope

In: repository hygiene, build conventions, versioning, PR and main pipelines, scanners,
CodeRabbit configuration, `main` ruleset and repository settings.

Out: GraalVM native image (post-MVP, preview features make it fragile), Helm, deployment.

## Design

### Repository hygiene

- Make `main` build: run Spotless, fix the agent test compilation, replace reflection-based
  tests with behaviour tests, raise control plane coverage to the gate.
- Remove: root `Dockerfile`, `Dockerfile-fast`, `Dockerfile.native`, duplicated `ISSUE_TEMPLATE/`,
  `.github/profile/`, committed `__pycache__`.
- Rename the space-prefixed issue templates; create the labels listed in `LABELS.md`.
- Add `.editorconfig`, `CONTRIBUTING.md` (points to the site), `CODE_OF_CONDUCT.md`
  (Contributor Covenant), `SECURITY.md`, `CODEOWNERS`, `.github/dependabot.yml`.
- `gradlew` executable in git; license header holder `Dmytro Hryshchenko` on all files.
- Coverage gate 85% (line, branch, method) per module, generated code and the Spring Boot
  launcher excluded; `api` has no hand-written code and no gate.
- Agent exception: `main` carried ~50 % line and ~29 % branch coverage in the agent, mostly in
  code that M1 replaces. The hygiene PR sets a per-module ratchet (`coverage.minimum.*` in
  `architrace-agent/gradle.properties`) at those values; every PR may only raise it and M1
  ends at the 85 % default. This is the only module below the default.

### Build conventions (`build-logic/`)

Gradle included build with convention plugins applied by every module:

| Plugin | Provides |
|--------|----------|
| `architrace.java` | JDK 25 toolchain, preview flag, Spotless (palantir-java-format + header), Checkstyle, JaCoCo with the gate, JUnit platform |
| `architrace.versioning` | version from git, Conventional Commits bump, `printVersion`, `printReleaseVersion`, `releaseNotes` ([ADR 0006](../../adr/0006-versioning-and-release-flow/)) |
| `architrace.spring-boot` | Spring Boot application defaults, build info, layered jar |

Gradle project paths equal directory names (`:architrace-agent`, not `:agent`). The CLI
`version` command reads `Implementation-Version` from the manifest.

### Pipelines

```
pr.yml        build ──► quality ──► security       (required checks)
              └ docs
pr-title.yml  title                                (required check)
codeql.yml    CodeQL (java-kotlin | javascript-typescript | actions)
main.yml      build ──► quality ──► security ──► release ──► images ──► docs deploy
```

- `build`: `./gradlew build`, the same gate as on a developer machine (compile, Spotless,
  Checkstyle, tests with the JaCoCo gate, UI gate, `build-logic` tests, jars); uploads the reports
  and the jars as artifacts.
- `quality`: SonarCloud analysis over the reports restored from `build`, quality gate on new code;
  skipped for Dependabot, which has no access to the token.
- `security`: Snyk test (high+), OWASP Dependency-Check (fail on CVSS ≥ 7, NVD API key from
  `NVD_API_KEY`, SARIF to code scanning), gitleaks over the commits of the pull request.
- `docs`: builds the documentation site.
- `title`: the pull request title follows the commit convention; its own workflow, so a title
  edit does not rebuild.
- `release` (main only): compute version; when the commits since the last tag contain a
  releasing type, create tag `vX.Y.Z`, GitHub release with generated notes.
- `images` (main only): build agent and control plane images, Trivy scan (fail on critical),
  push to GHCR as `X.Y.Z`, `latest`, `sha-<short>`.
- One composite action for JDK + Gradle setup; all actions pinned by commit SHA; concurrency
  groups cancel superseded PR runs.

### Review and repository settings

- `.coderabbit.yaml`: assertive profile, review on push, path instructions for Java,
  TypeScript, Gradle, workflows and docs, request-changes workflow on, summaries off (the PR
  template carries the summary).
- `main` ruleset per [ADR 0003](../../adr/0003-review-identity-and-merge-gate/): PR required, 0 approvals,
  required checks `build`, `quality`, `security`, `docs`, `title` and the CodeQL analyses,
  conversation resolution, linear history, no force push or deletion, no bypass.
- Repository: squash merge only, delete branch on merge, Dependabot alerts and security
  updates, secret scanning with push protection, code scanning via CodeQL.

## Acceptance criteria

- A PR with a failing test, a formatting violation or a high vulnerability cannot be merged.
- A merge of a `feat` commit to `main` produces a tag, a release with notes, two images in GHCR
  and an updated site; a `docs` merge produces no release.
- CodeRabbit reviews every non-draft PR and its threads block the merge until resolved.
- `./gradlew build` on a clean checkout is green with JDK 25.

## Delivery plan

1. Hygiene: green build on `main`, stale files removed, templates and labels fixed (ARCHI-24, #26).
2. `build-logic` conventions and versioning; CLI version from manifest (ARCHI-33).
3. `pr.yml`, `pr-title.yml`, `codeql.yml`, composite action, scanner configuration (ARCHI-34).
4. `main.yml` with release and images; Dockerfiles for agent and control plane (shared with M7).
5. Ruleset, repository settings, `.coderabbit.yaml`, site pages (contributing, getting started).

### PR 2: build conventions and versioning (ARCHI-33)

What landed and the decisions behind it:

- **Included build `build-logic`** with three precompiled script plugins. `architrace.java`
  (toolchain from `javaVersion`, `--enable-preview`, test dependencies from the catalog,
  Spotless, Checkstyle, JaCoCo report and gate wired into `check`, manifest
  `Implementation-Title` / `Implementation-Version`), `architrace.spring-boot` (Boot and
  dependency management on top of `architrace.java`, build info without the timestamp, plain jar
  disabled), `architrace.versioning` (axion-release with the Conventional Commits incrementer).
  Third-party plugin versions stay in `gradle/libs.versions.toml`; `build-logic` puts the plugins
  on the classpath, so module build files apply them by id without versions. The root build
  script shrank to the lifecycle wiring and the Sonar properties.
- **Project paths equal directory names** (`:architrace-agent`, …) as ADR 0005 requires, so
  artifacts are named `architrace-agent-<version>-all.jar` and
  `architrace-control-plane-<version>.jar`. Every command in the docs, the workflows and the
  demo Dockerfiles moved with them.
- **Versioning** ([ADR 0006](../../adr/0006-versioning-and-release-flow/)): axion-release finds
  the last `v*` tag; `ConventionalCommitsIncrementer` reads the commits since that tag through
  JGit and applies the highest bump (`!` or `BREAKING CHANGE` → major, `feat` → minor, `fix`,
  `perf`, `refactor`, `build` → patch, anything else → none). Between tags the version is
  `X.Y.Z-<sha>-SNAPSHOT`, on the tag `X.Y.Z`; without any tag it stays `0.1.0-<sha>-SNAPSHOT`
  until the maintainer seeds `v0.1.0`. Tasks: `printVersion`, `printReleaseVersion`,
  `releaseNotes` (Markdown grouped by type, scope in bold, `#NN` linked to the pull request,
  `-PreleaseNotes.since=<tag>` to pick the base). `gradle.properties` carries no version; the
  CLI (`version`, `--version`) and the agent registration read `Implementation-Version` from
  the manifest through `core.BuildVersion`. The parsing and the git reading are unit-tested in
  `build-logic` with JUnit and a temporary JGit repository; root `test` and `check` run them.
- **Formatting**: palantir-java-format through Spotless 8 (4-space indentation, 120 columns;
  `.editorconfig` follows). google-java-format was tried first and does not start under JDK 25
  without extra JVM exports. The reflow touched the control plane and the API module. The agent
  keeps `java.format.enabled=false` next to its coverage ratchet: reformatting its ~50 %-covered
  legacy code would turn every line into "new code" for the SonarCloud pull-request gate; M1
  rewrites the module and removes both exceptions. Until then Spotless only orders the agent's
  imports. The SPDX header is now followed by a blank line in every language.
- **Checkstyle 14** with `config/checkstyle/checkstyle.xml`: Google style minus everything the
  formatter owns (indentation) and everything Javadoc-related, plus rules the project needs:
  no `//` or `/**` comments (P7), no unused imports or local variables, `_` allowed as an unnamed
  catch or lambda parameter, single-line `{}` bodies allowed, no `default` branch requirement
  (switches are exhaustive by construction). Running it on the existing code found commented-out
  dead code and three empty test shells in the agent, which were removed, and a few spacing
  slips that were fixed by hand.
- **One gate command**: `./gradlew spotlessApply check`. `check` now runs Spotless, Checkstyle,
  the tests with the JaCoCo gate, the UI gate and the build-logic tests; the legacy workflows
  call `check` until PR 3 replaces them. Jobs that build artifacts fetch the full history so the
  version resolves.
- **Dropped**: the GraalVM native plugin (out of scope for the MVP, and it broke the OpenAPI
  generator's YAML parser), the root `runArchitrace` and `buildRuntime` tasks, the plain
  control plane jar (it collided with the `*.jar` glob of the demo Dockerfile).

### PR 3: pull request pipeline (ARCHI-34)

What landed and the decisions behind it:

- **`pr.yml`** replaces `agent.yml`. `build` runs `./gradlew build --continue`: the same command
  and therefore the same gate as locally, and every module reports its failures in one run; the
  test, coverage and Checkstyle reports and the two jars are uploaded as artifacts. `quality`
  downloads the reports, compiles and runs the SonarCloud analysis, then waits for the quality
  gate. The `sonar` task of the Sonar Gradle plugin has no task dependencies and only reads what
  is on disk, which is why the job compiles explicitly and restores the reports instead of
  re-running the tests; the plugin derives the JaCoCo XML path, the JUnit results path and the
  pull request coordinates by itself. `security` runs its scanners independently of each other
  (`if: !cancelled()`), so a Snyk failure never hides a leaked secret. `docs` builds the site.
  The job names are the required-check names of ADR 0003.
- **Composite action** `.github/actions/setup-build`: reads `javaVersion` and `nodeVersion` from
  `gradle.properties` (one source of truth for the toolchains), installs the Temurin JDK,
  configures Gradle with the dependency and build caches (written only from `main`) and caches the
  Node.js distribution that Gradle downloads into `.gradle/nodejs`.
- **Title check** in its own workflow `pr-title.yml` on `opened`, `edited`, `reopened` and
  `synchronize`: a title edit re-runs a two-second job instead of the whole pipeline. The regular
  expression accepts the nine commit types, the scope `ARCHI-<n>` or `deps` (Dependabot) and an
  optional `!`. The title reaches the script through an environment variable, never by
  interpolation.
- **CodeQL** (`codeql.yml`, advanced setup) on pull requests, pushes to `main` and weekly:
  `java-kotlin` in manual build mode (`./gradlew --no-daemon --no-build-cache compileJava`, so the
  tracer sees every javac invocation), `javascript-typescript` and `actions` without a build.
  `.github/codeql/codeql-config.yml` keeps generated sources, bundles and `node_modules` out of the
  results. Default setup is not enabled on the repository, so the workflow is the only analysis.
- **Snyk** tests every manifest it finds (`--all-projects`) except the Python demo and
  `build-logic` (no wrapper in that directory), fails on high and critical findings and is skipped
  with a notice when the token is absent (Dependabot runs).
- **OWASP Dependency-Check** through the Gradle plugin on the root project:
  `dependencyCheckAggregate` scans the `runtimeClasspath` of every module and the UI lockfile,
  fails at CVSS 7 and writes HTML and SARIF to `build/reports/dependency-check`; the SARIF goes to
  code scanning. The NVD data lives in `~/.gradle/dependency-check-data` and is cached per week.
  The task runs with `--no-parallel`: the aggregate task resolves the configurations of the other
  projects, which Gradle 9 forbids while projects execute in parallel. Suppressions belong in
  `config/dependency-check/suppressions.xml`.
- **gitleaks** runs as the pinned binary (checksum verified) over the commits of the pull request
  (`base..head`) with secrets redacted in the log. The official action was not used because it
  requires a licence key for organisation repositories.
- `docs-deploy.yml` runs only on pushes to `main` (pull requests build the site in `pr.yml`), with
  pinned actions and Node.js 24. `ci-cd.yml` stays until PR 4 replaces it with `main.yml`.

## Risks and open points

- CodeQL support for Java 25 preview sources shows on the first run of `codeql.yml`; the
  fallback is `build-mode: none`. SpotBugs stays out: Sonar and CodeQL cover bug and security
  patterns.
- Dependency-Check 13.0.0 cannot update without an NVD API key (upstream issue 8715 sends an
  empty key); the step is skipped with a warning until the maintainer stores the key as the
  repository secret `NVD_API_KEY`, and as a Dependabot secret so Dependabot pull requests are
  checked too. The first run with the key verifies the analysis end to end.
- Snyk runs only where `SNYK_TOKEN` is available: add it as a Dependabot secret as well.
- Dependabot pull request #41 (GitHub Actions bumps) edits the removed and rewritten workflows
  and needs a rebase or a close after PR 3.
- Sonar project key is still the old `…_Architrace-agent`; rename in SonarCloud or keep.
- The demo Dockerfiles build inside the container and therefore need `.git` in the build
  context for the version (removed from `.dockerignore`); PR 4 switches the images to prebuilt
  jars.
- Checkstyle does not analyse `build-logic` (Kotlin); its sources carry the SPDX header through
  Spotless and are covered by their own tests. Its package is `io.github.architrace.conventions`:
  a package segment named `build` is swallowed by the `**/build` ignore patterns of Docker and
  git, which broke the first in-container build.
- The Kotlin compiler embedded in Gradle 9.3 emits JVM 24 bytecode at most, so `build-logic`
  pins both Java and Kotlin tasks to 24; drop the pin when Gradle's Kotlin supports 25.
