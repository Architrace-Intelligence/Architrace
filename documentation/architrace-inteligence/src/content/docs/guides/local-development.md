---
title: Local Development
description: Run Architrace modules directly from source.
---

## 1. Start control-plane

```bash
./gradlew :architrace-control-plane:bootRun
```

## 2. Validate agent config (dry-run)

```bash
java --enable-preview -jar architrace-agent/build/libs/architrace-agent-*-all.jar dry-run --config ./demo/agent-dev.yaml
```

`dry-run` validates the file, applies `--prop key=value` overrides and prints the effective configuration.

## 3. Start agent runtime

```bash
java --enable-preview -jar architrace-agent/build/libs/architrace-agent-*-all.jar run --config ./demo/agent-dev.yaml --prop control-plane.server=localhost:9090
```

## 4. Send OTLP traces

Use your own instrumented service or the demo stack in `demo` (its collector forwards to the agents of the stack; point a service at your local agent on `4319` to see it here).

Agent receives trace exports on port `4319` and forwards graph events to control-plane on `9090`.

## 5. Run tests

```bash
./gradlew check
./gradlew :architrace-agent:test
./gradlew :architrace-control-plane:test
./gradlew :architrace-ui:test
./gradlew -p build-logic test
```

`check` is the full gate: Spotless (palantir-java-format and the SPDX header), Checkstyle
(`config/checkstyle/checkstyle.xml`), the tests with the JaCoCo coverage gate, the UI gate and
the build-logic tests. `:architrace-ui:test` runs `npm run check` in `architrace-ui`:
TypeScript, ESLint, Prettier and Vitest with coverage thresholds. The control plane tests need
the UI bundle on the classpath, so Gradle builds it first.

## 6. Develop the UI

```bash
cd architrace-ui
npm ci
npm run dev
```

The dev server proxies `/api` to the control plane on `8085`. Formatting is `npm run format`;
the generated `src/api/schema.d.ts` is refreshed from the OpenAPI document by `npm run generate`
and is never committed.

## 7. Change dependencies

Dependency versions are declared in `gradle/libs.versions.toml` and locked per module in
`gradle.lockfile` (Gradle dependency locking). After adding, removing or bumping a dependency,
rewrite the lock state and commit the lockfiles together with the change:

```bash
./gradlew dependencies :architrace-api:dependencies :architrace-agent:dependencies :architrace-control-plane:dependencies :architrace-ui:dependencies --write-locks
./gradlew -p build-logic dependencies --write-locks
```

Without `--write-locks` the build fails when a resolved version differs from the lockfile, so
transitive upgrades never slip in unnoticed. Dependabot updates the lockfiles in its PRs.

UI dependencies live in `architrace-ui/package.json` and are locked in `package-lock.json`;
change them with `npm install <package>` (or `npm install <package> --save-dev`) inside
`architrace-ui` and commit both files. Dependabot keeps them current as well.

## 8. Versions and release notes

The version is never edited by hand ([ADR 0006](../../project/adr/0006-versioning-and-release-flow/)):

```bash
./gradlew printVersion          # 0.2.0-3f9c1ab-SNAPSHOT between tags, 0.2.0 on the tag v0.2.0
./gradlew printReleaseVersion   # the version a release from this commit would get
./gradlew releaseNotes          # build/release-notes.md from the commits since the previous tag
```

`feat` raises the minor version, `fix`, `perf`, `refactor` and `build` the patch version, a `!`
after the scope or a `BREAKING CHANGE:` footer the major version; `ci`, `docs`, `test` and
`chore` do not release. Without any tag the version stays `0.1.0-<sha>-SNAPSHOT` until the
maintainer creates the first tag `v0.1.0`. `releaseNotes` accepts `-PreleaseNotes.since=<tag>`
to start from another tag, for example after the release tag has already been created.
