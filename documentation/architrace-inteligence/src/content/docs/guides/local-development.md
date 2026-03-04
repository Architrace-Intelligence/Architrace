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
```
