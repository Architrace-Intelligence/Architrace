---
title: M4. Service map UI
description: An interactive per-environment map of services, data stores, topics and external dependencies.
---

Status: in progress (PR 1 of 4 in review, ARCHI-32) · Order: 5 · Requirements: F8

## Goal

An architect opens one page, picks an environment and sees the real architecture: what talks
to what, over which channel, with what traffic and latency, and when.

## Scope

In: UI module and build integration, environment switcher, service map with layout, filters,
node and edge detail panels, time selector, serving from the control plane.

Out: drift view (M5), findings view (M6), editing, authentication.

## Design

Visual design and interaction: [UI design](../ui-design/) (interactive prototypes).

### Module and build

- `architrace-ui`: Vite, React, TypeScript strict, ESLint, Prettier, Vitest with Testing
  Library ([ADR 0004](../../adr/0004-react-typescript-ui/)).
- Gradle drives `npm ci`, `npm run check` (types, lint, tests) and `npm run build`; the
  bundle is copied into the control plane jar under `static/` and served with an SPA fallback
  for non-API routes.
- Types are generated from the OpenAPI document with `openapi-typescript`; requests use
  `openapi-fetch` (typed, no runtime code generation). Server state with TanStack Query.

### Graph rendering

Recommendation: **React Flow** with **ELK** layered layout. It gives first-class React nodes
(custom cards per node type), pan and zoom, selection and minimap, and ELK handles 300+ nodes
with readable left-to-right layering. Alternative kept in view: Cytoscape.js if layout quality
on large graphs proves insufficient. Final choice is confirmed in PR 2 with a 300-node fixture.

### Views

| View | Content |
|------|---------|
| Environment switcher | environments from the API, remembered in the URL |
| Service map | nodes styled by type (service, database, topic, external), edges styled by kind (sync solid, publish and consume dashed), edge width by calls, colour by error rate; minimap; fit to view |
| Filters | domain multi-select, node type toggles, text search with highlight, "hide externals" |
| Node panel | identity, versions seen, clusters and namespaces, inbound and outbound dependencies with metrics |
| Edge panel | calls, errors, error rate, p50 / p95 / p99 / max latency for the window |
| Time selector | "live" (latest) or a timestamp; the map re-queries `graph?at=` |

URL carries environment, filters and time so a view can be shared.

### Quality

- Component tests for filters and panels; a rendering test with a fixture graph.
- Lighthouse budget: initial bundle under 500 kB gzipped.
- Accessibility: keyboard navigation between nodes, visible focus, sufficient contrast.

## Acceptance criteria

- A 300-node, 900-edge fixture renders in under 2 s and stays interactive.
- Filters, search and the time selector change the map without a full reload.
- The UI is served from the control plane jar at `/` with no separate deployment.

## Delivery plan

The initial release is the two screens agreed on 2026-10-01 ([UI design](../ui-design/#initial-release)):
the Projects list and the Service map of a scope.

1. ARCHI-32 (merged as #39): module scaffold, Gradle integration, SPA serving, typed client,
   UI quality gate.
2. Projects list on `GET /scopes` with filters and grouping, with the shell (navigation rail,
   top bar).
3. Service map on `…/graph` and `…/services` with ELK layout (library decision).
4. Lenses, node and dependency panels, time selector, URL state, polish.

Ticket numbers are assigned when a branch is created (next free `ARCHI-<n>`); the remaining M0
pull requests and M1 come first (maintainer, 2026-10-02).

### PR 1: scaffold (ARCHI-32)

What landed and the decisions behind it:

- **Module `architrace-ui`** is a Gradle project (`:ui`) built with the `com.github.node-gradle.node`
  plugin. Gradle downloads the Node.js version pinned in `gradle.properties` (`nodeVersion`)
  into `.gradle/nodejs/` at the repository root, so a contributor needs no local Node to build
  or test; the dev loop (`npm run dev`) needs Node 22.12+ and proxies `/api` to port 8085.
- **One gate, two entry points.** `npm run check` chains the TypeScript types from the OpenAPI
  document, `tsc -b`, ESLint (flat config, type-checked rules of typescript-eslint, React hooks
  and refresh rules, Prettier conflicts disabled), `prettier --check` and Vitest with Testing
  Library and V8 coverage thresholds of 85 %. Gradle runs it as `:ui:test`, so the existing
  pipelines and the documented quality-gate command cover the UI without changes.
- **Bundle hand-over as a Gradle artifact.** `:ui:npmBuild` writes `build/dist`, exposed as the
  consumable configuration `bundle`; the control plane resolves it into `processResources`
  under `static/`. No task reaches into another project.
- **Single-page fallback.** `WebConfiguration` registers `/assets/**` (hashed files, one-year
  immutable cache) and `/**` (`no-cache`) with a `SpaFallbackResourceResolver` that extends
  Spring's `PathResourceResolver`: when the requested file does not exist, the path is not
  under `/api`, `/actuator`, `/swagger-ui` or `/webjars`, its last segment has no extension and
  the client accepts `text/html`, the resolver answers `index.html`; everything else keeps its
  404, so the API still returns problem details and clients asking for JSON never get HTML.
  The configurer has the highest precedence so Spring Boot does not register its own `/**`.
- **Typed client without code generation.** `openapi-typescript` writes `src/api/schema.d.ts`
  from the contract (never committed); `openapi-fetch` gives typed `GET` calls at zero runtime
  cost; a thin `listScopes` turns problem responses into a `ProblemError` carrying the RFC 9457
  body. TanStack Query holds server state through `queryOptions` factories.
- **Design tokens** from the Foundations screen are the CSS custom properties in
  `src/styles/tokens.css` (dark by default, light under `data-theme="light"`); IBM Plex Sans and
  JetBrains Mono are self-hosted through Fontsource. The direction still awaits the
  maintainer's confirmation; changing it is a token edit.
- **Toolchain pins.** TypeScript stays on 5.9 because `openapi-typescript` and
  `typescript-eslint` do not support 6 and 7 yet; Vite 8, Vitest 5, React 19, ESLint 10.
  The Vite template's default linter (oxlint) was not adopted: the agreed design names ESLint.
- **Spotless** applies the SPDX header to `.ts`, `.tsx`, `.css` and the config files of the UI,
  so `spotlessCheck` covers both languages; Prettier keeps the formatting.
- **SonarCloud** analyses `architrace-ui/src` with the Vitest `lcov` report; Dependabot watches
  `architrace-ui/package.json` weekly.
- **Walking skeleton.** The first screen shows the `Projects` heading and how many scopes the
  control plane reports (or the problem it answered); PR 2 replaces it with the list.

## Risks and open points

- Node tooling in CI lengthens the pipeline (Node download and `npm ci` per job, about a
  minute); caching `~/.npm` is a follow-up for the M0 pipeline work.
- Graph library choice is validated early with real data from the demo stack.
- The UI tests stub `fetch` globally; the client therefore resolves `globalThis.fetch` per
  call instead of capturing it at creation.
