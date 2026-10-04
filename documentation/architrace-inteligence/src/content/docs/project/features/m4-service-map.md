---
title: M4. Service map UI
description: An interactive per-environment map of services, data stores, topics and external dependencies.
---

Status: in progress (PR 3 in review as #56, PR 4 stacked on it as ARCHI-45) · Order: 5 · Requirements: F8

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

Decision (PR 3, ARCHI-44): **React Flow 12** with the **ELK layered** algorithm. React Flow
gives first-class React nodes (one card component per node type), pan and zoom, minimap and
controls; ELK layers the graph left to right so a dependency always points right. Cytoscape.js
stays the fallback if layout quality on real landscapes disappoints.

The 300-node, 900-edge fixture (`src/test/graph.ts`: 180 services in 6 tiers, 60 data stores,
40 topics, 20 external hosts) measured on the maintainer's machine with Node 24:

| Graph shape | ELK options | Layout time |
|-------------|-------------|-------------|
| service-shaped fixture | layered, defaults | 0.75–0.83 s |
| service-shaped fixture | layered, thoroughness 3 (chosen) | 0.54 s |
| service-shaped fixture | layered, thoroughness 1 | 0.42 s |
| random 300/900 | layered, defaults | 1.8 s |
| random 300/900 | layered, thoroughness 3 | about 0.75 s |

ELK is loaded on demand: the bundled engine is its own chunk (436 kB gzipped) fetched the first
time a map opens, so the initial bundle stays at 156 kB gzipped against the 500 kB budget. In
the production build served by `vite preview` in Chromium, opening the fixture scope from the
Projects list took 1.4 s from the click until all 300 cards and 900 edges were in the DOM,
including the first load of the ELK chunk; the map pans and zooms freely afterwards.

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

- A 300-node, 900-edge fixture renders in under 2 s and stays interactive (measured 1.4 s in
  the production build, see Graph rendering).
- Filters, search and the time selector change the map without a full reload (every control
  writes the URL through the router; the page tests assert it).
- The UI is served from the control plane jar at `/` with no separate deployment.

## Delivery plan

The initial release is the two screens agreed on 2026-10-01 ([UI design](../ui-design/#initial-release)):
the Projects list and the Service map of a scope.

1. ARCHI-32 (merged as #39): module scaffold, Gradle integration, SPA serving, typed client,
   UI quality gate.
2. ARCHI-43 (merged as #55): Projects list on `GET /scopes` with filters and grouping, with the
   shell (navigation rail, top bar).
3. ARCHI-44 (#56): Service map on `GET …/graph` with React Flow and ELK, node-type chips,
   legend, minimap, fit to view.
4. ARCHI-45 (stacked on #56): lenses, context rail with the scope, node and dependency panels,
   find-in-map, time selector, selection and time in the URL, keyboard selection, copy link.

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

### PR 2: Projects list (ARCHI-43)

What landed and the decisions behind it:

- **Routing.** `react-router` 8 in declarative mode (`BrowserRouter`, `Routes`): `/` is the
  Projects list, `/scopes/{project}/{environment}/{cluster}` the scope page (a breadcrumb and
  the request behind the future map until PR 3), anything else falls back to the list. The
  library is the first dependency decision of the UI after the scaffold: it is the mainstream
  choice, its `useSearchParams` is exactly the "URL carries the filter" principle, and it
  stays out of data loading, which TanStack Query already owns.
- **Shell.** The navigation rail (Projects, Map, theme toggle) and the top bar (title, a tools
  slot for the filter field, "Open as JSON" to the Query API request behind the screen) from
  the frames; the context rail waits for the map. The theme toggles `data-theme` on the
  document and remembers itself in `localStorage` behind a `try`/`catch`, so a blocked storage
  only loses the preference. Below 860 px the rail becomes a top row, as the design says.
- **Pure list logic** (`projects/filters.ts`): the filter is a value parsed from and written to
  the URL (`env`, `cluster`, `q`, `group`; defaults are omitted), and every operation is a pure
  function over `ScopeSummary[]`: filtering, facet values with per-value counts computed with
  the other facets applied (so a value that would yield nothing shows `0`, faint, but stays
  selectable), grouping by project, environment or cluster with sorted keys and rows, and the
  summary of the count line. The components only render.
- **Components.** `FacetChip` is a chip with a popover of toggles (`aria-pressed`), closed by
  Escape or a click outside; `ScopeGroups` renders one `section` per group with a real table,
  the first column linking to the scope page; environment as a badge, cluster in mono, agents
  as `n live`, `n live of m` or a `stale` badge, the last snapshot as relative time computed
  once per mount. The count line is an `output` element, so assistive technology announces
  filter changes.
- **Not in this PR.** Region and findings columns of the frames: the API has no region source
  and findings come with M6. The Ask bar is a plain filter field; questions come with an
  insights provider (N9).
- **Tests.** The pure logic has unit tests for every function; the page tests drive the real
  router and the typed client with a stubbed `fetch` through user events (chips, popover,
  grouping, search, clear, empty and error states, link targets); the shell test covers the
  theme and the map entry. Testing Library's `user-event` is the only new dev dependency.
  Coverage of the UI stays above 95 % on every counter.

### PR 3: Service map (ARCHI-44)

What landed and the decisions behind it:

- **One request.** The scope page asks `GET /scopes/{project}/{environment}/{cluster}/graph`
  once (`graphQuery`, TanStack Query) and derives everything else in the browser: node-type
  counts, the visible subgraph, edge styles. `GET …/services` is not called: every dependency
  it would return is already in the graph, and the panels of PR 4 derive inbound and outbound
  lists from the same edges (YAGNI).
- **Pure map logic** (`map/model.ts`): the filter is the set of hidden node types, parsed from
  and written to the URL (`hide=DATABASE`); `visibleGraph` drops hidden nodes and every edge
  that touches them; `edgeHealth` applies the design thresholds (amber from 1 % errors, red
  from 3 %); `edgeWidth` grows with `log10(calls)` and caps at 6 px; `subtitle` prefers the
  versions, then the namespaces, then the id. Components only render.
- **Layout as derived server state** (`map/layout.ts`, `ServiceMap`): `layoutGraph` turns the
  visible graph into an ELK graph (fixed 192 × 56 cards, direction `RIGHT`, thoroughness 3)
  and returns a map of positions. The component runs it through `useQuery` keyed by the node
  and edge ids, so a layout is computed once per visible graph, cached while the user toggles
  chips back and forth, and never recomputed inside an effect. The engine is imported
  dynamically, which gives Vite the split point for the ELK chunk.
- **Encoding in CSS, not in JavaScript.** React Flow edges carry two class names, the kind
  (`edge-sync`, `edge-publish`, `edge-consume`) and the health (`health-ok`, `health-warn`,
  `health-bad`); the stylesheet maps them to dash pattern and colour through the design tokens,
  so both themes work without any colour logic in components. Arrowheads are three SVG markers
  owned by the map (`arrow-ok`, `arrow-warn`, `arrow-bad`) filled by the same tokens. The React
  Flow theme variables (`--xy-*`) are mapped onto the tokens once in `base.css`.
- **Node cards** are a custom React Flow node type (`card`): icon tile by type, name, subtitle
  in mono, invisible handles on the left (target) and right (source) so every edge enters from
  the left and leaves to the right. Node type is announced to assistive technology through a
  visually hidden label; the node id is the tooltip.
- **Chrome of the canvas.** Node-type chips with counts above the map (pressed = shown,
  dashed = hidden, URL carries the hidden set), a count line as an `output` element, legend
  bottom-left, minimap bottom-right coloured by node type, zoom and fit-to-view controls
  top-right, dotted background on the design grid. Nodes are not draggable in this release:
  positions come from the layout and are not persisted anywhere.
- **Tests.** The pure logic has unit tests; `layout.test.ts` checks that dependencies layer
  left to right on the demo graph and that the 300-node fixture lands on 300 distinct positions
  (correctness only: the same layout took 1.2 s on the maintainer's machine and 9.4 s on a
  GitHub runner, so the 2 s acceptance criterion is measured in the browser, not asserted in
  CI); the page tests drive the real router and the
  typed client with a stubbed `fetch` through the chips, the hidden set in the URL, the empty
  scope and the problem response, and count the rendered edges by class. React Flow needs a
  `ResizeObserver`, `DOMMatrixReadOnly` and element sizes that jsdom lacks; `src/test/setup.ts`
  stubs them, and the observer callback is deferred to a microtask because React Flow registers
  its container after the node effects run.
- **Not in this PR.** Selection and the context rail, the Data streams lens, find-in-map, the
  time selector and URL state for selection and time come with PR 4. The Ask bar slot of the top
  bar holds the breadcrumb on this screen.

### PR 4: Lenses, panels and time (ARCHI-45)

What landed and the decisions behind it:

- **One URL state.** `MapState` (`map/model.ts`) holds everything the screen remembers: the
  hidden node types (`hide`), the lens (`lens=streams`), the find text (`q`), the point in time
  (`at`) and the selection (`node=<id>` or `edge=<source>><target>:<kind>`, a discriminated
  union). `parseMapState` and `toMapParams` are the only two places that know the parameter
  names; defaults are omitted, an invalid `at` is ignored, and a selection that is not in the
  visible graph is ignored by the rendering.
- **Looks as one pure pass.** `looks(visible, state)` derives per-node flags (`selected`,
  `dimmed`, `match`) and per-edge flags (`touching`, `dimmed`) from the selection, the query
  and the lens together: a selected node lights itself, its neighbours and the touching edges;
  a selected edge lights its two ends; a query lights its matches; the Data streams lens
  recedes synchronous calls and every node without a stream. Components only turn flags into
  class names, and the metric pills of the design are React Flow edge labels placed on the
  touching edges.
- **Context rail.** The shell gains an `aside` slot (360 px, hidden below 1180 px). The
  scope panel summarises the graph and lists its data streams with producers and consumers;
  the node panel shows identity, namespaces, versions, clusters, labels, inbound and outbound
  totals, the streams a service publishes and consumes (or the producers and consumers of a
  topic) and its synchronous dependencies; the dependency panel shows calls, errors, error
  rate with the health badge and the latency percentiles. Every row is a button that moves
  the selection, so the rail is navigable without the canvas: a dependency row opens the edge,
  an end of an edge opens the node, a stream opens the topic. The panels derive everything
  from the graph through `nodeDetails`, `edgeDetails` and `streams`; `GET …/services` is still
  not needed.
- **Time.** The time selector is a popover with a `datetime-local` field read as UTC; `at`
  goes into `graphQuery(scope, at)` and therefore into `GET …/graph?at=` and the "Open as
  JSON" link. Live is the absence of `at`. Layouts stay cached by node and edge ids, so a graph
  that did not change between two instants does not lay out twice.
- **Keyboard.** React Flow makes every card focusable; Enter and Space select the focused
  card through a key handler on the canvas (React Flow calls `onNodeClick` for pointers only),
  Escape clears the selection, and the rail rows are buttons. The visible focus ring follows
  the card, not the React Flow wrapper.
- **Copy link** sits in the top bar of every screen (the URL is the view): the clipboard API,
  "Copied" for a moment. **Find in map** is the Ask bar slot of this screen; it dims
  non-matching nodes and the count line reports the matches.
- **`usePopover`** was extracted from the facet chip for the time selector. It takes the
  container ref as an argument instead of returning one, because the React Compiler lint rules
  treat a hook result that carries a ref as a ref and forbid reading it during render.
- **Not in M4.** Findings and insights in the rail (M6), agent liveness in the scope panel
  (M7 demo), node dragging, a web worker for ELK.
- **Tests.** `model.test.ts` covers the URL round trip, the details, the stream summaries and
  every rule of `looks`; `metrics.test.ts` the formatting; `ContextPanel.test.tsx` the three
  panels and their buttons; `TimeSelector.test.tsx` apply, prefill, live and Escape;
  `Shell.test.tsx` the copy link; `ScopePage.test.tsx` selection from the URL and by click,
  keyboard selection, the lens, find and the point in time through the real router, React Flow
  and the typed client with a stubbed `fetch`.

## Risks and open points

- Node tooling in CI lengthens the pipeline (Node download and `npm ci` per job, about a
  minute); caching `~/.npm` is a follow-up for the M0 pipeline work.
- ELK runs on the main thread: a 300-node layout blocks for well under a second once, when the
  graph loads. A web worker is the follow-up if real landscapes grow past that.
- Layout quality on real landscapes is validated with the demo stack of M7; Cytoscape.js stays
  the fallback.
- The UI tests stub `fetch` globally; the client therefore resolves `globalThis.fetch` per
  call instead of capturing it at creation.
