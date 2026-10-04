---
title: UI design
description: Screens, foundations and agent-first principles of the MVP web UI, delivered as interactive prototypes in Claude Design.
---

Status: design ready for review · Covers: the user interface of M4, M5 and M6 plus the overview
and agents screens · Requirements: F8, F9, F10, N9

## Goal

One coherent interface for the whole MVP: an architect opens it, picks an environment and sees
the real architecture, what differs from another environment or from yesterday, and what the
rules say about it. The MVP ships without any AI, but the interface is shaped so that an AI
agent can take the wheel later without a redesign.

## Where the design lives

- Interactive prototypes: Claude Design project **Architrace UI** ([open](https://claude.ai/design/p/a93f3239-c308-4f32-b561-051ac09fbe06?file=Start-here.dc.html)).
  Eight screens, each a working prototype on demo data: nodes are clickable, environments
  switch, the Ask bar filters, the theme toggles. Start with `Start-here`.
- Sources: [`design/ui-prototype/`](https://github.com/Architrace-Intelligence/Architrace/tree/main/design/ui-prototype)
  in the repository, rebuilt with `python3 design/ui-prototype/src/build.py`.

| Screen | File | Purpose |
|--------|------|---------|
| Start here | `Start-here.dc.html` | index of the design, direction, demo data, implementation notes |
| Overview | `Overview.dc.html` | all environments at a glance: counts, agent health, findings, differences, recent changes |
| Service map | `Service-map.dc.html` | per-environment map with filters, search, time selector, node and dependency panels |
| Drift | `Drift.dc.html` | environment and timeline comparison as grouped list and as map overlays |
| Findings | `Findings.dc.html` | findings grouped by rule with evidence, thresholds and passing rules |
| Agents | `Agents.dc.html` | registered agents, liveness, throughput, configuration they run with |
| Foundations | `Foundations.dc.html` | colour tokens in both themes, type, spacing, node and edge encoding, components |
| Agent-first | `Agent-first.dc.html` | the four principles and the honest boundaries of the MVP |

## Initial release

On 2026-10-01 the maintainer narrowed the first UI release to two screens: a **Projects** list
that filters by project, environment and Kubernetes cluster, and the **Service map** of the
chosen scope with every service and its data streams. The other prototype screens (Overview,
Drift, Findings, Agents) remain the design for later releases.

The initial release is designed as static frames on one canvas, `MVP-frames.dc.html` in the
same Claude Design project ([open](https://claude.ai/design/p/a93f3239-c308-4f32-b561-051ac09fbe06?file=MVP-frames.dc.html));
the source is [`design/ui-frames/`](https://github.com/Architrace-Intelligence/Architrace/tree/main/design/ui-frames).

| Frame | Shows |
|-------|-------|
| 1 · Projects | one row per scope (project × environment × cluster) grouped by project: namespaces, services, data streams, agents, findings by severity, last snapshot; filter chips for environment, cluster, region, agents and findings; group by project, environment or cluster |
| 2 · Projects, filtered | environment = PROD, grouped by cluster, the cluster facet open with per-value counts; the URL carries the filter |
| 3 · Service map | the scope opened from a row (breadcrumb project / environment / cluster), node-type chips with counts, the Lens control (All, Data streams); the context rail summarises the scope and lists its data streams with producers and consumers |
| 4 · Service selected | neighbours stay lit, metric pills on the touching edges; the rail shows version, namespace, inbound and outbound dependencies, the streams the service publishes and consumes, and its findings |
| 5 · Data stream selected | the Data streams lens recedes sync calls; the rail shows broker, producers, consumers, fan-out and a timeline insight (a consumer added yesterday) |

A scope is project × environment × cluster; the map can also open for all clusters of an
environment. A project is a configured grouping of services above the environment (see the
open points). Data streams are topics and queues with their producers and consumers (C10);
they are first-class nodes on the map and a section of every service panel.

## Direction

"Calm control room": dark by default for long sessions beside terminals and dashboards, with a
complete light theme. One cool accent for interaction, four hues reserved for node types, red
and amber reserved for severity and drift. IBM Plex Sans for text, JetBrains Mono for
identifiers and numbers. Dense where data lives, generous where decisions are made.

| Encoding | Rule |
|----------|------|
| Node type | icon plus hue: service, database, topic, external |
| Edge kind | solid for sync calls, dashed for publish and consume, arrow at the callee |
| Edge weight | stroke width grows with calls on a log scale |
| Edge health | neutral below 1% errors, amber from 1%, red from 3% |
| Selection | accent ring on the node, accent on touching edges, metric pills on those edges |
| Drift | `+` only on the right side, `−` only on the left (ghost node), `Δ` version differs |
| Severity | badge with the word: high, medium, low; colour reinforces, never carries, the meaning |

All colours are CSS custom properties with the same names in both themes; text keeps 4.5:1
against its ground, graphics 3:1. The Foundations screen lists every token with its value.

## Shell

| Region | Width | Content |
|--------|-------|---------|
| Navigation rail | 84 px | Overview, Map, Drift, Findings, Agents, theme toggle |
| Top bar | 56 px | screen title, environment switcher where it applies, the Ask bar, time selector on the map, copy-link and open-as-JSON |
| Content | fluid | the screen |
| Context rail | 360 px | what is selected: environment, node, dependency, comparison, agent |

The context rail hides below 1180 px, the navigation rail below 860 px, where the top bar
wraps and the Ask bar takes the full width. Controls grow to 44 px targets on small screens.

## Screens

### Service map (M4)

Environment switcher, domain select, node-type chips with counts, find-in-map, time selector
(live or a point in time). Nodes show type, name, version or system and a findings badge.
Selecting a node dims everything not adjacent, colours its edges accent and shows metric pills
on them; the context rail shows identity, versions and replicas, cluster and namespace, inbound
and outbound dependencies with metrics, findings on the node and its version history.
Selecting a pill opens the dependency panel: calls, errors, p50, p95, p99, max, window, edge
key and the reporting agent. A legend and a minimap sit in the corners. URL state carries
environment, filters, time and selection.

### Drift (M5)

Two modes in one segmented control: environments (left and right pickers with swap) and
timeline (environment, from, to). Five counters state the semantics explicitly: nodes only on
the right, nodes only on the left, version changes, dependencies only on the right or left.
The list view groups differences by services, data stores, topics, external and dependencies,
with "Show on map" switching to the map view where overlays mark added, removed and changed
nodes and edges. The context rail turns the diff into sentences and, when a dependency on one
side makes a database shared where it lands, says which rule it would raise.

### Findings (M6)

Severity chips and a rule select filter a list grouped by rule; each group states the
threshold and the configuration property behind it. Rows expand to the detail, the evidence
rows and actions: show on map, and for unknown externals the allowlist line to copy. Rules that
pass are listed too, so silence is never ambiguous. The context rail shows severity totals and
every rule with its threshold, property and status.

### Overview and Agents

Overview is the landing screen: one card per environment with counts, agent health, findings by
severity and the number of differences to PROD (or to STAGE for PROD), followed by recent
changes derived from the snapshot series. Agents lists registered agents with liveness, last
snapshot, throughput, dropped spans, services seen and version; the context rail shows the
configuration an agent runs with and what a stale agent means for the graph.

## Agent-first principles

1. **One bar, two eras.** The Ask bar is on every screen. Today it understands commands and
   names; later it accepts questions. The field, the result list and the links do not move.
2. **The context rail is the agent's seat.** Every card has four slots: source, claim, evidence,
   actions. Rules, diffs and the snapshot series fill them now; a provider writes into the same
   slots later. Every card is labelled "deterministic" until then.
3. **Everything is addressable.** Environment, filters, time and selection live in the URL;
   node ids never contain the environment; every screen shows the Query API request behind it.
4. **Evidence before verdict.** Findings show paths, calls and windows; drift shows which side
   has what; metrics are never part of drift. An agent that later speaks for the product cites
   these rows instead of asserting.

What the MVP does not pretend to do is written on the screen: no natural-language input, no
narrative insights (C12), no remote agent configuration (C15), no declared architecture (C13).

## Demo data

Eight services across five domains, four data stores, two Kafka topics, two external hosts,
three environments and six agents. PROD carries a synchronous cycle (orders and inventory) and
a shared database (reporting-service reads postgresql/orders). DEV is one release ahead:
search-service 2.3.0 reads postgresql/catalog directly, which would create a second shared
database if promoted. The figures are fixtures shaped to exercise every rule and both drift
modes; they are not measurements.

## Implementation mapping

- The shell, badges, chips, insight card, tabs and table become the first React components;
  their CSS custom properties are the token file.
- Map nodes are React Flow custom nodes with the card layout above; edges carry kind, calls and
  error rate as data and derive stroke, dash and colour; ELK layers left to right
  ([M4](../m4-service-map/)).
- Diff rows and map overlays consume the `TopologyDiff` model unchanged ([M5](../m5-drift/));
  the "makes the database shared" sentence is a pure function over the diff and both graphs.
- Findings groups mirror the `Finding` record and the rule configuration ([M6](../m6-architecture-rules/)).
- URL state (environment, filters, time, selection, drift sides) is the only client state that
  survives a reload; server state comes from the Query API through TanStack Query.

## Open points

- The Projects list introduces *project* as a grouping above the environment. The MVP model
  (ADR 0007) carries environment, domain, cluster and namespace but no project; the frames
  assume a configured source (a resource attribute such as `service.namespace`, or an agent
  setting). Decide the source before M3 fixes the Query API.
- The aesthetic direction was chosen without a brief: dark-first, cool accent, Plex and
  JetBrains Mono. Confirm or redirect before implementation starts.
- Environment drift compares versions only. Deployments (cluster, namespace) differ between
  environments by construction, so comparing them in environment mode would mark every node
  as changed; the design proposes comparing deployments in timeline mode only. ARCHI-48
  implements it this way (`DiffMode` in [M5](../m5-drift/)); the maintainer confirms or
  redirects.
- Fan-in hub and long sync chain never fire on the demo landscape; the demo stack of M7 should
  include a case for each so the findings page is exercised end to end.
- Allowlisting an external is a configuration change in the MVP; the UI offers the property
  line to copy. A write path can come with remote configuration (C15).
- Phone-width layouts work but are not a design goal; the map is a desktop surface.
