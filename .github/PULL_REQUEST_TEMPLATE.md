<!--
Title format: <type>(ARCHI-<n>): <Imperative subject>
Types: feat | fix | perf | refactor | build | ci | docs | test | chore   (add "!" for breaking)
The title becomes the squash commit on main and drives the next version number.
-->

## Summary

<!-- One paragraph: what problem this PR solves and why now. Link the feature doc if one exists. -->

Task: ARCHI-
Feature doc: `docs/features/<name>.md`

## Type of change

- [ ] Feature
- [ ] Bug fix
- [ ] Refactoring
- [ ] Build / CI
- [ ] Documentation
- [ ] Tests only
- [ ] Breaking change (describe migration below)

## What was done

<!-- Technical description for a reviewer who has not seen the code: components touched,
     new classes / modules, data flow, contracts changed. Bullet points. -->

-

## Patterns and approaches

<!-- Name every pattern, principle or technique used and in one line why it fits here
     (e.g. "sealed interface + pattern matching for span classification: closed set of
     node types, compiler-checked exhaustiveness"). Write "none" for trivial changes. -->

-

## Architecture impact

- [ ] No structural change
- [ ] `docs/architecture.md` updated
- [ ] ADR added or updated: `docs/adr/`
- [ ] Public API / protobuf contract changed
- [ ] New dependency added (name, reason)

## Testing

<!-- What was run locally and what the tests cover. Paste the exact commands. -->

- [ ] Unit tests added / updated
- [ ] Integration tests added / updated
- [ ] `./gradlew spotlessApply classes test jacocoTestReport jacocoTestCoverageVerification` green
- [ ] Manual verification (describe)

## Documentation

- [ ] `docs/progress.md` updated
- [ ] Feature doc updated
- [ ] README / docs site updated (if user-facing)

## Notes for the reviewer

<!-- Risks, trade-offs, follow-ups, anything to look at first. -->
