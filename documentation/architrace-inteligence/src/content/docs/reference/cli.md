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
java -jar architrace-agent/build/libs/agent-0.1.0-all.jar version
java -jar architrace-agent/build/libs/agent-0.1.0-all.jar dry-run --config ./otel-test-app/architrace-agent.yaml
java -jar architrace-agent/build/libs/agent-0.1.0-all.jar run --config ./otel-test-app/architrace-agent.yaml
```

## Notes

- `run` starts OTLP receiver and control-plane lifecycle.
- `dry-run` currently performs limited validation flow logging.
