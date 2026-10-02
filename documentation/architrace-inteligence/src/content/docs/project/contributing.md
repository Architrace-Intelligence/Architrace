---
title: Contributing
description: "How changes reach main: branches, commits, pull requests, reviews, releases."
---

The full operating rules are in
[`AGENTS.md`](https://github.com/Architrace-Intelligence/Architrace/blob/main/AGENTS.md) at the
repository root. This page is the short human version.

## Flow

1. Every change starts from an agreed feature (see [Requirements](../requirements/)) and a
   branch from `main` named `ARCHI-<n>-<topic>`. Ticket numbers are sequential: take the
   highest number used in branches and PR titles and add one.
2. Work is tested locally with all quality gates green:

   ```bash
   ./gradlew spotlessApply check
   ```

3. A pull request is opened with the repository template filled in: summary, what was done,
   patterns and approaches used, architecture impact, testing, documentation.
4. CI runs formatting, compilation, tests with coverage, static analysis, security scanning and
   an automated AI code review by an independent agent.
5. The maintainer reviews and merges (squash); the merge is the approval, see
   [ADR 0003](../adr/0003-review-identity-and-merge-gate/). The main pipeline builds, versions
   and publishes artifacts and this site.

## Commit and PR title convention

Conventional Commits with the ticket as scope:

```
<type>(ARCHI-<n>): <Imperative subject>
```

`feat` bumps the minor version, `fix` / `perf` / `refactor` / `build` bump the patch version,
`docs` / `test` / `ci` / `chore` do not release. A `!` after the scope or a `BREAKING CHANGE:`
footer bumps the major version.

## Documentation rules

- Everything readers need is on this site; it is updated in the same PR as the change.
- Architecture decisions are recorded as ADRs in the [decision log](../adr/) only after the
  maintainer has agreed them.
- No documentation comments in code: names, types and structure carry the meaning.
