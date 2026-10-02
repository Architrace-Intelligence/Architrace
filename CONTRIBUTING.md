# Contributing to Architrace

Thank you for your interest. Architrace is developed in the open and every change, including
the maintainer's own, goes through a pull request with automated checks and review.

The full process is documented on the site:

- [Contributing](https://architrace-intelligence.github.io/Architrace/project/contributing/):
  branches, commit convention, pull request flow, review gates.
- [Current state](https://architrace-intelligence.github.io/Architrace/project/state/) and
  [Progress](https://architrace-intelligence.github.io/Architrace/project/progress/): what is
  being worked on right now.
- [Requirements](https://architrace-intelligence.github.io/Architrace/project/requirements/):
  agreed scope and feature pages.

Operating rules for AI agents and contributors are in [`AGENTS.md`](./AGENTS.md).

## Quick checklist

1. Open or pick an issue; agree the scope before writing code for anything non-trivial.
2. Branch from `main`: `ARCHI-<n>-<topic>`.
3. Run the quality gates locally with JDK 25:

   ```bash
   ./gradlew spotlessApply check
   ```

4. Open a pull request titled `<type>(ARCHI-<n>): <Imperative subject>` and fill in the
   template completely.
5. Resolve every review thread; the maintainer merges.

By contributing you agree that your contributions are licensed under the Apache License 2.0.
