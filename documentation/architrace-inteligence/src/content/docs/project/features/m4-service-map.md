---
title: M4. Service map UI
description: An interactive per-environment map of services, data stores, topics and external dependencies.
---

Status: design proposed · Order: 5 · Requirements: F8

## Goal

An architect opens one page, picks an environment and sees the real architecture: what talks
to what, over which channel, with what traffic and latency, and when.

## Scope

In: UI module and build integration, environment switcher, service map with layout, filters,
node and edge detail panels, time selector, serving from the control plane.

Out: drift view (M5), findings view (M6), editing, authentication.

## Design

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

1. Module scaffold, Gradle integration, SPA serving, CI gates for the frontend.
2. API client, environment switcher, service map with ELK layout (library decision).
3. Filters, search, node and edge panels, time selector, URL state.
4. Visual polish, accessibility pass, user guide page.

## Risks and open points

- Node tooling in CI lengthens the pipeline; mitigated by caching `node_modules`.
- Graph library choice is validated early with real data from the demo stack.
