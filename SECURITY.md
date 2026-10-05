# Security policy

## Supported versions

Architrace is pre-1.0. Only the latest release and `main` receive security fixes.

## Reporting a vulnerability

Please do not open a public issue for security problems.

Report privately through GitHub:
**Security → Advisories → Report a vulnerability** on the repository
(https://github.com/Architrace-Intelligence/Architrace/security/advisories/new).

Include the affected component (agent, control plane, UI, demo), a description, reproduction
steps and the impact you expect. You will receive an acknowledgement within 7 days and a
status update at least every 14 days until the report is resolved.

## Scope

- Agent: OTLP receiver, control plane client, configuration handling.
- Control plane: gRPC ingestion, HTTP API, UI, persistence.
- Build and release pipeline, published container images.

Out of scope: the demo services in `demo/`, which exist only to generate
sample telemetry.

## Automated scanning

Dependencies, code and images are scanned in CI (Snyk, OWASP Dependency-Check, CodeQL, Trivy,
Gitleaks). Findings are triaged as issues with the `security` label.
