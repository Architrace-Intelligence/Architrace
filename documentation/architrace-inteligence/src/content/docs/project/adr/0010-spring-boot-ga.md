---
title: 0010. Pin Spring Boot and Spring gRPC to GA releases
description: Only released versions from Maven Central; no milestone or snapshot repositories.
---

Status: proposed
Date: 2026-10-01

## Context

The control plane build declares Spring milestone and snapshot repositories although the
declared Spring Boot and Spring gRPC versions are released. Snapshot repositories make builds
non-reproducible and let unreleased changes into an open-source product.

## Decision

- Dependencies resolve from Maven Central only; the milestone and snapshot repositories are
  removed from every build file.
- Spring Boot stays on the current GA line (4.0.x) and Spring gRPC on its GA line; Dependabot
  proposes upgrades, which are merged through the normal PR flow.
- A pre-release version is used only if a required fix is not yet released, documented in the
  version catalogue with the issue link and removed as soon as the release is out.

## Consequences

- Reproducible builds and a clear upgrade trail in git.
- New Spring features arrive with the GA release, not before.

## Alternatives considered

- **Keep milestone and snapshot repositories**: earliest access to features, rejected for
  reproducibility and supply-chain hygiene.
