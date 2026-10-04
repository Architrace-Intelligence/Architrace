---
title: Comparing environments and releases
description: How to read the Drift screen and its two modes, and how to get the same answer from the Query API.
---

Drift answers two questions about the graphs Architrace has observed: *what is different
between two environments* and *what changed in one environment since a point in time*. Both
are one comparison of two graphs; only the sides differ.

## Open the screen

Open a scope from the Projects list, then choose **Drift** in the navigation rail (or append
`/drift` to the scope URL). The scope you came from is the **right side** of the comparison;
the breadcrumb switches it like on the map. Everything the screen shows is in the URL, so
**Copy link** gives a colleague the same comparison and **Open as JSON** shows the Query API
request behind it.

## Two modes

| Mode | Left side | Right side | A node is "changed" when |
|------|-----------|------------|--------------------------|
| **Environments** | another scope of the same project, picked in the toolbar (another environment, or another cluster of the same environment) | the scope in the breadcrumb | its set of versions differs |
| **Timeline** | the scope at **From** | the scope at **To** (default now) | its versions or its deployments (cluster, namespace) differ |

The sentence under the pickers states the direction: everything "only in" the right side is
new on the right. **Swap** turns the comparison around; the time control sets the instant both
sides are resolved for in the Environments mode.

## Reading the result

Five counters summarise the comparison: nodes only on the right (`+`), nodes only on the left
(`−`), changed nodes (`Δ`), dependencies only on the right and only on the left.

The **List** view groups every difference by services, data stores, data streams, external
hosts and dependencies. A row names the node (or the two ends of a dependency) and says on
which side it exists, or which version it runs on each side. **Show on map** opens the
**Map** view with that node selected.

The **Map** view draws the right side's graph and overlays the differences: nodes and
dependencies only on the right are green, nodes only on the left are drawn as red ghosts with
dashed borders, changed nodes are amber and show both versions. The legend names the sides.

The context rail turns the diff into sentences (added, missing, changed, dependencies) and
describes the selected node. Nodes are matched by their environment-independent id, so a
rename shows as removed plus added, which is the truthful answer. Traffic metrics are never
compared: volume is not drift.

## From a script

```bash
curl -s 'http://localhost:8085/api/v1/scopes/webshop/PROD/k8s-prod-eu1/diff/environments?leftEnvironment=DEV&leftCluster=k8s-dev' | jq .
curl -s 'http://localhost:8085/api/v1/scopes/webshop/PROD/k8s-prod-eu1/diff/timeline?from=2026-09-30T18:00:00Z' | jq .nodesChanged
```

The response shape and the error types are in the [Query API reference](../../reference/query-api/#drift).
