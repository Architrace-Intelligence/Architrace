---
title: M0. Engineering platform
description: Green gates on every PR, automatic versioning and publishing on main.
---

Status: in progress (PR 5 of 5 in review, ARCHI-37) · Order: 1 · Requirements: P1–P7, §5.1–5.5

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
main.yml      build ──► quality ──► security ──► release ──► images (agent | control-plane)
              └ dependency graph                  └ docs deploy
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
- `release` (main only): `printReleaseVersion` against the latest `v*` tag; when they differ, write
  the release notes, tag `vX.Y.Z`, rebuild the jars under the tag and publish the GitHub release
  with the notes and both jars. Without any tag the job only reports that `v0.1.0` has to be
  seeded.
- `images` (main only): build the agent and control plane images from the prebuilt jars, upload
  a Trivy SARIF to code scanning, fail on fixable critical findings, push to GHCR as
  `sha-<short>` on every merge and additionally as `X.Y.Z` and `latest` on a release.
- `docs` (main only): build and deploy the site to GitHub Pages after the gates; `build` also
  submits the Gradle dependency graph so Dependabot alerts cover the Java dependencies.
- One composite action for JDK + Gradle setup; all actions pinned by commit SHA; concurrency
  groups cancel superseded PR runs.

### Review and repository settings

- `.coderabbit.yaml`: assertive profile, review on push, path instructions for Java, tests,
  TypeScript, Gradle, workflows, contracts, Dockerfiles and docs, request-changes workflow on,
  summaries and poems off (the PR template carries the summary), docstring generation and the
  docstring coverage check off (P7 forbids documentation comments), `AGENTS.md` as the coding
  guideline document.
- `main` ruleset per [ADR 0003](../../adr/0003-review-identity-and-merge-gate/), kept as code
  in `.github/rulesets/main.json`: PR required, 0 approvals, required checks `build`, `quality`,
  `security`, `docs`, `title` and the three CodeQL analyses, conversation resolution, squash as
  the only merge method, linear history, no force push or deletion, no bypass, plus the CodeQL
  code scanning threshold. The maintainer applies the file; the commands are on the
  [GitHub setup](../../github-access/#5-the-main-ruleset-and-the-repository-settings) page.
- Repository: squash merge only with the pull request title as the commit subject, delete
  branch on merge, Dependabot alerts and security updates, secret scanning with push
  protection, code scanning via CodeQL.

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
4. `main.yml` with release and images; Dockerfiles for agent and control plane (shared with M7) (ARCHI-36).
5. Ruleset, repository settings, `.coderabbit.yaml`, site pages (contributing, getting started) (ARCHI-37).

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
  `build-logic` is compiled by a plain `./gradlew help` before `codeql init`: under the tracer the
  Kotlin compile daemon receives the CodeQL Kotlin extractor and ran out of its default heap, and
  the convention plugins are not analysis targets anyway.
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

### PR 4: main pipeline, release and images (ARCHI-36)

What landed and the decisions behind it:

- **`main.yml`** replaces `ci-cd.yml` and `docs-deploy.yml`; `build`, `quality` and `security`
  are the same jobs as in `pr.yml` (plus `snyk monitor` to keep the Snyk project snapshot
  current, gitleaks over `before..sha` of the push and the full history on manual dispatch), and
  `build` passes `dependency-graph: generate-and-submit` to the composite action so GitHub's
  dependency graph and Dependabot alerts know the Gradle dependencies. The concurrency group
  `main` never cancels a run: a release in flight must finish.
- **`release`** runs after the three gates. It compares `printReleaseVersion` (last line of a
  quiet Gradle run) with `git describe --tags --match 'v*'`: no tag at all → notice to seed
  `v0.1.0` and no release (ADR 0006 keeps the first tag manual); equal → nothing releasable
  (docs, ci, chore merges); different → `releaseNotes` first (its default base is the previous
  tag, so it must run before tagging), then the annotated tag is pushed with `GITHUB_TOKEN`
  (events from that token start no workflows, so nothing recurses), the jars are rebuilt under
  the tag so they carry `X.Y.Z` in their names and manifests, and `gh release create` publishes
  the notes with both jars attached. The job exports `released` and `version` for the images.
- **`images`** is a matrix over the two modules. It downloads `release-jars` or the SNAPSHOT
  `jars`, lowercases the registry path (GHCR rejects the capitalised organisation name), builds
  with `load: true` and tags from `docker/metadata-action` (`sha-<short>` always, `X.Y.Z` and
  `latest` only when released), scans twice with Trivy (a SARIF with critical and high findings
  for the Security tab, then a gate on fixable critical findings) and only then pushes every tag.
- **Dockerfiles** live next to the modules and copy the prebuilt jar; the build context is the
  module directory with a `.dockerignore` that admits only the jar. Base image
  `eclipse-temurin:25-jre-alpine` pinned by digest (Dependabot's `docker` ecosystem keeps tag and
  digest current), a system user `architrace`, `JDK_JAVA_OPTIONS` with `MaxRAMPercentage=75` and
  `ExitOnOutOfMemoryError`, `--enable-preview` in both entrypoints, OCI labels with the version
  from a build argument. The control plane exposes `8085` and `9090` and checks
  `/actuator/health`; the agent exposes `4317`, checks a TCP connect to `ARCHITRACE_OTLP_PORT`
  (its only health signal is gRPC) and expects the configuration at
  `/config/architrace-agent.yaml` (`run --config` has no default; override `CMD` to change it).
  Local images need a fresh `build/libs` (`./gradlew clean assemble`) because the `COPY`
  pattern must match exactly one jar. Verified locally: the agent prints its version, the control
  plane with PostgreSQL turns healthy within 15 seconds, both run as uid 100.
- The demo's in-container Dockerfiles are gone; the commented services in
  `otel-test-app/docker-compose.yml` now point at the module Dockerfiles.

### PR 5: merge gate, repository settings and CodeRabbit (ARCHI-37)

What landed and the decisions behind it:

- **Ruleset as code.** `.github/rulesets/main.json` is the exact request body of the rulesets
  API and the import format of the GitHub UI, so the gate is reviewed in a pull request like
  everything else and can be re-applied after a change of job names. Required checks are bound
  to the GitHub Actions app id, so a check with the same name from another app cannot satisfy
  them. Skipped jobs count as passed, which is what lets Dependabot pull requests through
  `quality` without the SonarCloud token. `strict_required_status_checks_policy` stays off: work
  lands one pull request at a time, and `main.yml` runs the same gates on the merge commit, so
  the "branch up to date" requirement would only add rebases of long-lived Dependabot branches.
  The CodeQL `code_scanning` rule from the old disabled ruleset is kept (no new alert of
  severity error, no new security alert of high or higher); its `copilot_code_review` and
  `code_quality` rules are dropped, the first needs a paid plan, the second is a preview product
  outside the agreed design.
- **Squash commit shape.** The title of the squash commit is the pull request title, which the
  title check already validates against the commit convention, so the Conventional Commits
  incrementer reads a guaranteed-valid header on every `main` commit. The body keeps the
  squashed commit messages so a `BREAKING CHANGE:` footer written in any commit still reaches
  the parser (`ConventionalCommit.parse` scans the body with a multi-line regular expression).
- **Applying the settings is a maintainer action.** The agent session's tool permissions stop at
  administration writes, and the ruleset would gate its own pull request anyway. The GitHub setup
  page carries the two commands; the state page tracks them as the open step of M0.
- **CodeRabbit configuration.** Validated against the published schema
  (`https://coderabbit.ai/integrations/schema.v2.json`). The assertive profile and the
  request-changes workflow turn findings into blocking threads; summaries, poems, fortunes,
  label and reviewer suggestions and the legacy commit status are off because the PR template,
  the maintainer and the ruleset cover them. Docstring generation and the docstring coverage
  pre-merge check are off (no documentation comments, P7), the title pre-merge check is off
  (`pr-title.yml` already enforces it). Lockfiles and the wrapper jar are filtered out of the
  review. One path instruction per language or artefact type restates the rules of `AGENTS.md`
  that a reviewer can check line by line (Stream API, no comments, exhaustive switches, test
  style, pinned actions, contract compatibility), and `AGENTS.md` itself is declared as the
  coding guideline document. The app is installed by the maintainer (organisation owner).
- **Site pages.** Contributing lists the checks a pull request has to pass and the ruleset that
  enforces them; Getting started points to the guides and the contributing page; the GitHub
  setup page carries the observed state, the ruleset explanation and the maintainer checklist.
- **Images fix.** The first `main` run after PR 4 failed in both `images` jobs: the Trivy steps
  referenced the image as `sha-<full sha>` while `docker/metadata-action` had tagged it
  `sha-<short sha>`, so the scan found no image and the push never ran. Both scans now use the
  `version` output of the metadata step, which is the same tag the build applied (`sha-<short>`
  on a plain merge, `X.Y.Z` on a release). One source of truth for the tag instead of two
  expressions that have to agree.

## Risks and open points

- CodeQL extracted the Java 25 sources in manual build mode on the first run of PR 3; the
  fallback, should a preview construct break the extractor later, is `build-mode: none`. SpotBugs
  stays out: Sonar and CodeQL cover bug and security patterns.
- Dependency-Check 13.0.0 cannot update without an NVD API key (upstream issue 8715 sends an
  empty key); `NVD_API_KEY` exists as a repository and a Dependabot secret since 2026-10-02 and
  the step is skipped with a warning wherever it is absent.
- Snyk runs only where `SNYK_TOKEN` is available (repository and Dependabot secret). The PAT
  expires on 2026-12-31 and has to be rotated before.
- Open maintainer actions after PR 5: apply the ruleset and the repository settings, install
  the CodeRabbit app, seed `v0.1.0`, make the GHCR packages public once the first successful
  `images` run has created them (they start private), close Dependabot #45 (it edits the
  removed `ci-cd.yml`). Until the ruleset is active the gate is discipline, not platform.
- Dependabot cannot regenerate Gradle lockfiles, so its Gradle pull requests fail `build`;
  dependency bumps are taken over in maintainer pull requests until a lockfile-refresh step
  exists.
- Sonar project key is still the old `…_Architrace-agent`; rename in SonarCloud or keep.
- Checkstyle does not analyse `build-logic` (Kotlin); its sources carry the SPDX header through
  Spotless and are covered by their own tests. Its package is `io.github.architrace.conventions`:
  a package segment named `build` is swallowed by the `**/build` ignore patterns of Docker and
  git, which broke the first in-container build.
- The Kotlin compiler embedded in Gradle 9.3 emits JVM 24 bytecode at most, so `build-logic`
  pins both Java and Kotlin tasks to 24; drop the pin when Gradle's Kotlin supports 25.
