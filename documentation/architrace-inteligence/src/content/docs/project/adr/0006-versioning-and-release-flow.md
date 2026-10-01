---
title: 0006. Semantic versioning from Conventional Commits and the release flow
description: Versions are computed from git tags and commit types; main merges release automatically.
---

Status: proposed
Date: 2026-10-01

## Context

The version is hard-coded in `gradle.properties` and in the CLI, no tag has ever been created,
and the process requires that every merge to `main` yields versioned artifacts without manual
steps (P2). Commit subjects already follow Conventional Commits with the ticket as scope.

## Decision

- A `build-logic` convention plugin built on `axion-release` computes the version: last tag
  `vX.Y.Z` plus the highest bump found in commits since that tag (`!` or `BREAKING CHANGE` →
  major, `feat` → minor, `fix` / `perf` / `refactor` / `build` → patch, others → none).
- Between tags the version is `X.Y.Z-<shortSha>-SNAPSHOT`; on a tag it is `X.Y.Z`.
- Tasks: `printVersion`, `printReleaseVersion`, `releaseNotes` (Markdown grouped by type,
  ticket references linked).
- The main pipeline creates the tag and the GitHub release when the computed bump is not
  "none"; images and jars are published under that version. Documentation-only merges do not
  release.
- The CLI reports the version from the jar manifest; `gradle.properties` no longer carries one.

## Consequences

- PR titles must follow the convention because the squash commit subject is what the plugin
  reads; a title check runs in the PR pipeline.
- A wrong type in a title changes the next version; fixing it means a follow-up commit, not a
  history rewrite.
- The first release is `v0.1.0`, created manually once to seed the tag history.

## Alternatives considered

- **Manual version bumps in `gradle.properties`**: simple, rejected because it is forgotten
  and produces merge conflicts.
- **semantic-release or release-please**: mature, rejected to keep the version logic inside
  the Gradle build where every developer can run it, and to avoid a second commit-parsing
  implementation.
