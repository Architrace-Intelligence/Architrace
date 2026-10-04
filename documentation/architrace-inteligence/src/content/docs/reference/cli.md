---
title: CLI Commands
description: Architrace agent command reference.
---

## Binary

Main command: `architrace`

Subcommands:

- `version`
- `dry-run --config <path> [--prop key=value ...]`
- `run --config <path> [--prop key=value ...]`

`--config` points at the YAML file described on the [Configuration](../configuration/) page.
`--prop` overrides one value by its dotted path and may be repeated; the value is parsed as
YAML (`--prop otlp.port=4320`, `--prop "attribute-mapping.domain=[team, service.namespace]"`).

## Examples

```bash
java --enable-preview -jar architrace-agent/build/libs/architrace-agent-*-all.jar version
java --enable-preview -jar architrace-agent/build/libs/architrace-agent-*-all.jar dry-run --config ./otel-test-app/architrace-agent.yaml
java --enable-preview -jar architrace-agent/build/libs/architrace-agent-*-all.jar run --config ./otel-test-app/architrace-agent.yaml --prop otlp.port=4320
```

## Behaviour and exit codes

- `dry-run` loads the file, applies the overrides, validates everything and prints the effective
  configuration as YAML, including the default attribute mapping. A valid configuration exits
  with `0`; an invalid one prints every problem to stderr (`Configuration is invalid:` followed
  by one line per problem) and exits with `1`.
- `run` performs the same validation, then starts the OTLP receiver, the span pipeline and the
  control plane session. An invalid configuration exits with `1` before anything is started.
- A missing `--config` option exits with `2` and prints the usage.
