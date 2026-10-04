---
title: Current state
description: Where the work is right now and what the next step is. Read this first when resuming.
---

Hand-over page. It describes only the present: what is in flight, what is decided, what comes
next. History lives in [Progress](../progress/) and in git. Rewrite it before starting a new
task; delete anything that is no longer needed to resume.

Last updated: **2026-10-04**

## Where we are

- Stages 0–2 are done. Stage 3 (implementation) is open. Order of work since 2026-10-02: finish
  **M0** first, then M1, M4 PR 2 onwards, M5, M6, M7; the vertical UI slices are paused after
  M4 PR 1.
- Merged: M0 PR 1–4 (#26, #42, #44, #48), the dependency update (ARCHI-35, #46), the UI design
  (#30), M2 (#31, #33, #35), M3 (#36, #38), M4 PR 1 (#39). The UI direction (dark-first "calm
  control room", tokens in `architrace-ui/src/styles/tokens.css`) still awaits the maintainer's
  confirmation.
- **M0 PR 5 (ARCHI-37, in review)**: the `main` ruleset as code (`.github/rulesets/main.json`),
  `.coderabbit.yaml` validated against the CodeRabbit schema, the contributing, getting-started
  and GitHub setup pages, and a fix of `main.yml`: the first `main` run (2026-10-04) passed every
  gate and reported the missing seed tag, but both `images` jobs failed because Trivy scanned
  `sha-<full sha>` while the image was tagged `sha-<short sha>`; both scans now use the tag the
  metadata step computed. Details on the [M0](../features/m0-engineering-platform/) page.
- The agent session cannot write repository settings or rulesets (its tool permissions stop at
  administration writes), so applying them is a maintainer step, listed below.
- Collection processing uses the Stream API across both modules (maintainer, 2026-10-01;
  rule in `AGENTS.md` §4).
- Automation token for the GitHub API is issued and verified; git pushes use SSH.

## Decisions

Agreed and recorded (Requirements §7–8, ADR 0001–0010): PostgreSQL + Liquibase, CodeRabbit,
maintainer-authored PRs with a zero-approval ruleset, React + TypeScript UI, sequential
`ARCHI-<n>`, copyright holder. MVP scope M0–M7; design agreed on 2026-10-01. Order of work
since 2026-10-02: M0 to completion, then M1, M4, M5, M6, M7.

The initial UI release is narrowed to two screens (maintainer, 2026-10-01): a Projects list
filtered by project, environment and cluster, and the Service map of the chosen scope with
its services and data streams.

Query API conventions since ARCHI-31: domain errors carry `urn:architrace:problem:<slug>`
types, framework errors none; the services list embeds the dependencies of every service
instead of a per-id resource; the snapshot history filters on window end, inclusive.

UI conventions since ARCHI-32: feature folders under `src/`; server state only through
`queryOptions` factories; the generated `src/api/schema.d.ts` is never committed; tests stub
`fetch` with the helpers in `src/test/http.ts`; TypeScript stays on 5.x until
`openapi-typescript` and `typescript-eslint` support 6 and 7.

Build conventions since ARCHI-33: new Java modules apply `architrace.java` (or
`architrace.spring-boot`); third-party plugins are applied by id, their versions live in the
catalog and on the `build-logic` classpath; switches stay exhaustive instead of carrying a
`default`; the first release tag `v0.1.0` is created manually by the maintainer, everything
after it is computed.

Pipeline conventions since ARCHI-34: every job name is a required-check name; actions are pinned
by commit SHA with the version in a trailing comment (Dependabot keeps both current); a scanner
whose action needs a licence runs as a pinned, checksum-verified binary instead; a step that needs
a secret skips with a notice when the secret is absent (Dependabot runs) rather than failing;
the Gradle gate in CI is the same command as locally.

Merge gate conventions since ARCHI-37: the ruleset lives in `.github/rulesets/main.json` and
changes with the job names it requires; squash merge only, the pull request title is the commit
subject; branches need not be up to date with `main` (one pull request at a time); CodeRabbit
threads are resolved by a fix or an answer.

Working assumption since ARCHI-26: a **scope** is project × environment × cluster, reported by
the agent at registration and stored on every snapshot. Pending: where the project value comes
from on the agent side (resource attribute or agent setting), confirmation of the UI direction,
and the drift refinement proposed on the UI design page (compare deployments in timeline mode
only).

## Next step

M0 PR 5 (ARCHI-37) is open for review. Merge it first (one PR at a time). Then the maintainer
closes M0 with the checklist on the [GitHub setup](../github-access/#6-setup-checklist-for-the-maintainer)
page: apply the ruleset and the repository settings (§5 there), install the CodeRabbit app,
watch the `main` run of the merge (the `images` jobs must now push `sha-<short>` tags), make the
two GHCR packages public, seed `v0.1.0` on that merge commit, close Dependabot #45. After that,
update this page and mark M0 done on the progress page.

Then, in order (one PR each, next free ticket number after ARCHI-37):

1. M1 agent pipeline (restores 85 % coverage and the formatter in the agent); take over the
   Dependabot Gradle bumps of #47 in a maintainer PR on the way (Dependabot cannot regenerate
   the lockfiles).
2. M4 PR 2 (Projects list), M4 PR 3–4, M5, M6, M7.

## How to resume

```bash
git fetch --all --prune && git status --short && git branch --show-current
gh pr list --state open
./gradlew printVersion
```

Then read [Progress](../progress/) and the feature page of whatever is in flight. Local
toolchain notes are in `AGENTS.md` (JDK 25 path, Gradle wrapper, Node downloaded by Gradle).
