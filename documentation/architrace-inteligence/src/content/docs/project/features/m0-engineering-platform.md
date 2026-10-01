---
title: M0. Engineering platform
description: Green gates on every PR, automatic versioning and publishing on main.
---

Status: design agreed · Order: 1 · Requirements: P1–P7, §5.1–5.5

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
pr.yml    build ──► quality ──► security          (required checks)
          └ docs build (site)
codeql.yml (advanced setup, JDK 25, manual build mode)
main.yml  build ──► quality ──► security ──► release ──► images ──► docs deploy
```

- `build`: compile, unit and integration tests, coverage report, upload reports and jars.
- `quality`: Spotless, Checkstyle, Sonar with quality gate on new code.
- `security`: Snyk test (high+), OWASP Dependency-Check (fail on CVSS ≥ 7, NVD API key from
  `NVD_API_KEY`), Gitleaks.
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
  required checks `build`, `quality`, `security`, `CodeQL`, conversation resolution, linear
  history, no force push or deletion, no bypass.
- Repository: squash merge only, delete branch on merge, Dependabot alerts and security
  updates, secret scanning with push protection, code scanning via CodeQL.

## Acceptance criteria

- A PR with a failing test, a formatting violation or a high vulnerability cannot be merged.
- A merge of a `feat` commit to `main` produces a tag, a release with notes, two images in GHCR
  and an updated site; a `docs` merge produces no release.
- CodeRabbit reviews every non-draft PR and its threads block the merge until resolved.
- `./gradlew build` on a clean checkout is green with JDK 25.

## Delivery plan

1. Hygiene: green build on `main`, stale files removed, templates and labels fixed.
2. `build-logic` conventions and versioning; CLI version from manifest.
3. `pr.yml`, `codeql.yml`, composite action, scanner configuration, Dependabot.
4. `main.yml` with release and images; Dockerfiles for agent and control plane (shared with M7).
5. Ruleset, repository settings, `.coderabbit.yaml`, site pages (contributing, getting started).

## Risks and open points

- SpotBugs and CodeQL support for Java 25 preview bytecode must be verified in PR 3; fallback
  is Sonar + Error Prone for bug patterns.
- OWASP Dependency-Check without an NVD API key is slow; the maintainer creates the key.
- Sonar project key is still the old `…_Architrace-agent`; rename in SonarCloud or keep.
