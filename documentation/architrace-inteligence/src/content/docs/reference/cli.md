---
title: CLI Commands
description: Architrace agent command reference.
---

## Binary

Main command: `architrace`

Subcommands:

- `version`
- `dry-run --config <path> [--prop key=value]`
- `run --config <path>`

## Examples

```bash
java --enable-preview -jar architrace-agent/build/libs/architrace-agent-*-all.jar version
java --enable-preview -jar architrace-agent/build/libs/architrace-agent-*-all.jar dry-run --config ./otel-test-app/architrace-agent.yaml
java --enable-preview -jar architrace-agent/build/libs/architrace-agent-*-all.jar run --config ./otel-test-app/architrace-agent.yaml
```

## Notes

- `run` starts OTLP receiver and control-plane lifecycle.
- `dry-run` currently performs limited validation flow logging.
