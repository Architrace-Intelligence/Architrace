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
2. Work is tested locally with all quality gates green. It is the same command CI runs:

   ```bash
   ./gradlew spotlessApply check
   ```

3. A pull request is opened with the repository template filled in: summary, what was done,
   patterns and approaches used, architecture impact, testing, documentation. A pull request
   changes at most 12 files; larger work is split into sequential pull requests, one at a
   time.
4. CI runs the gates. Each one is a required check on `main`:

   | Check | What it does |
   |-------|--------------|
   | `build` | `./gradlew build`: formatting, Checkstyle, tests with the coverage gate, the UI gate, jars |
   | `quality` | SonarCloud analysis with the quality gate on new code |
   | `security` | Snyk, OWASP Dependency-Check, gitleaks |
   | `docs` | documentation site build |
   | `title` | the pull request title follows the commit convention |
   | `size` | the pull request changes at most 12 files |
   | `CodeQL (java-kotlin)`, `CodeQL (javascript-typescript)`, `CodeQL (actions)` | code scanning |

   CodeRabbit reviews every pull request ([ADR 0002](../adr/0002-coderabbit-ai-review/)) and
   opens threads; every thread is resolved, either by a fix or by an answer that says why the
   finding is not applied.
5. The maintainer reviews and squash-merges; the merge is the approval
   ([ADR 0003](../adr/0003-review-identity-and-merge-gate/)). The `main` ruleset, defined in
   `.github/rulesets/main.json`, enforces the gate: a pull request, the checks above, resolved
   threads, squash merge only, linear history, no force pushes, no deletions, no bypass. The
   main pipeline then versions and publishes the artifacts and this site.

Merged branches are deleted automatically. Follow-up work always goes to a new branch and a new
pull request, never onto a merged branch.

## Commit and PR title convention

Conventional Commits with the ticket as scope:

```
<type>(ARCHI-<n>): <Imperative subject>
```

The pull request title becomes the subject of the squash commit on `main`, so the title is what
the versioning reads. `feat` bumps the minor version, `fix` / `perf` / `refactor` / `build` bump
the patch version, `docs` / `test` / `ci` / `chore` do not release. A `!` after the scope or a
`BREAKING CHANGE:` footer in one of the squashed commits bumps the major version.

## Documentation rules

- Everything readers need is on this site; it is updated in the same PR as the change.
- Architecture decisions are recorded as ADRs in the [decision log](../adr/) only after the
  maintainer has agreed them.
- No documentation comments in code: names, types and structure carry the meaning.
