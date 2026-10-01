---
title: 0005. Monorepo module layout and dependency rules
description: Which modules exist, what each may depend on, and how packages are shaped inside them.
---

Status: proposed
Date: 2026-10-01

## Context

The repository already has three Gradle modules whose project paths (`:agent`) differ from
their directories (`architrace-agent`), server-side code lives inside the agent, and the
MVP adds a UI, build conventions and a demo stack. Contributors need one obvious place for
every kind of code and compiler-enforced boundaries where they matter.

## Decision

| Module | Contents | May depend on |
|--------|----------|---------------|
| `build-logic` | Gradle convention plugins (included build) | nothing in the repo |
| `architrace-api` | protobuf, OpenAPI document, generated code only | nothing in the repo |
| `architrace-agent` | agent application | `architrace-api` |
| `architrace-control-plane` | control plane application | `architrace-api`, the UI bundle as a build artifact |
| `architrace-ui` | frontend sources, built by Vite through Gradle | `architrace-api` (OpenAPI document) |
| `demo` | docker compose and sample services; not a Gradle project | published images |

- Gradle project paths equal directory names.
- No module depends on an application module; shared code goes to `architrace-api` only if
  it is contract, otherwise it is duplicated or extracted into a new library module.
- Inside applications, packages are organised by feature. Each feature package keeps domain
  types (records, sealed interfaces, no framework imports) separate from services and
  adapters. Framework annotations appear only in adapters and configuration.
- Generated code is never edited and never committed.
- Dependency versions live only in `gradle/libs.versions.toml`.

## Consequences

- One-time rename of Gradle project paths and of the agent's misplaced server classes.
- Reviews can reject framework imports in domain packages by rule; an ArchUnit test enforces
  the domain purity and the module dependency rules.
- Adding a feature means adding one package with the same inner shape, not a new module.

## Alternatives considered

- **Hexagonal split into separate domain / application / adapter modules** per application:
  stronger isolation, rejected for the MVP as unnecessary ceremony (YAGNI); the package rules
  plus ArchUnit give the same guarantee.
- **Single Gradle module**: simplest build, rejected because agent and control plane have
  different runtime stacks and release artifacts.
