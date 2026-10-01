---
title: 0002. CodeRabbit as the independent AI reviewer
description: Every pull request is reviewed by CodeRabbit before the maintainer reviews it.
---

Status: accepted
Date: 2026-10-01

## Context

The process requires a review by an independent agent before the maintainer's review
(requirement P3). The options compared were CodeRabbit, Qodo PR-Agent, Gemini Code Assist and
GitHub Copilot code review (Requirements §5.4). The repository is public, the budget for review
tooling is zero, and the review must be able to block a merge until its findings are handled.

## Decision

- CodeRabbit is installed as a GitHub App on the repository and reviews every pull request
  automatically.
- Its behaviour is configured in `.coderabbit.yaml` at the repository root: review profile,
  path instructions, which checks to run, auto-generated summaries off where they duplicate
  the PR template.
- The gate is enforced through the `main` ruleset: *require conversation resolution* means
  every CodeRabbit thread must be resolved (fixed or answered) before the maintainer can merge.

## Consequences

- The review runs outside GitHub Actions. The pipeline does not contain an `ai-review` job;
  the ruleset provides the gate instead.
- Findings are treated as work items: either fixed in the PR or answered in the thread with
  the reason they are not applied.
- The maintainer installs the app once (organisation owner action); no API key or secret is
  stored in the repository.
- If CodeRabbit changes its free tier for public repositories, Qodo PR-Agent with a
  bring-your-own key is the documented fallback.

## Alternatives considered

- **Qodo PR-Agent**: open source, runs as a workflow step, needs an LLM API key and incurs
  usage cost; kept as fallback.
- **Gemini Code Assist**: free for public repositories, weaker inline findings in comparisons.
- **GitHub Copilot code review**: requires a paid Copilot plan even for public repositories.
