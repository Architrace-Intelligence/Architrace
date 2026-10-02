---
title: Local Development
description: Run Architrace modules directly from source.
---

## 1. Start control-plane

```bash
./gradlew :control-plane:bootRun
```

## 2. Validate agent config (dry-run)

```bash
java -jar architrace-agent/build/libs/agent-0.1.0-all.jar dry-run --config ./otel-test-app/architrace-agent.yaml
```

`dry-run` currently logs validation start and accepts `--prop key=value` overrides (not yet applied).

## 3. Start agent runtime

```bash
java -jar architrace-agent/build/libs/agent-0.1.0-all.jar run --config ./otel-test-app/architrace-agent.yaml
```

## 4. Send OTLP traces

Use your own instrumented service or the demo collector in `otel-test-app`.

Agent receives trace exports on port `4319` and forwards graph events to control-plane on `9090`.

## 5. Run tests

```bash
./gradlew :agent:test
./gradlew :control-plane:test
./gradlew :api:test
./gradlew :ui:test
```

`:ui:test` runs `npm run check` in `architrace-ui`: TypeScript, ESLint, Prettier and Vitest with
coverage thresholds. The control plane tests need the UI bundle on the classpath, so Gradle
builds it first.

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
./gradlew dependencies :api:dependencies :agent:dependencies :control-plane:dependencies :ui:dependencies --write-locks
```

Without `--write-locks` the build fails when a resolved version differs from the lockfile, so
transitive upgrades never slip in unnoticed. Dependabot updates the lockfiles in its PRs.

UI dependencies live in `architrace-ui/package.json` and are locked in `package-lock.json`;
change them with `npm install <package>` (or `npm install <package> --save-dev`) inside
`architrace-ui` and commit both files. Dependabot keeps them current as well.
