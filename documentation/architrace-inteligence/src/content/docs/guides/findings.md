---
title: Reading the architecture findings
description: How to read the Findings screen, the eight deterministic rules behind it, and the blast radius of any node on the map.
---

Findings are what the architecture rules say about the current graph of a scope: a cycle of
synchronous calls, a database shared by several services, a node whose failure would stop
most of the system. The rules are deterministic: the same graph always yields the same
findings, with the evidence attached, and no model is involved.

## Open the screen

Open a scope from the Projects list, then choose **Findings** in the navigation rail (or
append `/findings` to the scope URL). The Projects list already shows how many findings a
scope has, per severity; the badge links to the screen. The filters live in the URL
(`severity`, `rule`), so **Copy link** gives a colleague the same view and **Open as JSON**
shows the Query API request behind it.

## When the rules run

The control plane evaluates the rules right after it has stored a snapshot of the scope, at
most once per scope per `architrace.rules.evaluation-interval` (30 s by default), and
replaces the findings of the scope as a whole. The line above the list says when the graph
the findings describe was evaluated. A scope that has not reported yet and a clean graph
both show **No findings**.

## Reading a finding

Findings are grouped by rule, high severity first. Every group states the default threshold
of its rule and the configuration property that changes it. A row names the problem and the
nodes it is about; expanding it shows the explanation, the **evidence** (the nodes that
substantiate the finding, in the order the rule chose: the cycle, the clients of the
database, the callers, the chain) and **Show on map**, which opens the map with the subject
selected. An unknown external host also offers the allowlist line to copy into the control
plane configuration.

The rules that found nothing are listed too, so silence is never ambiguous. The context rail
shows the totals per severity and every rule with its status, threshold and property.

## The eight rules

| Rule | Fires when | Default threshold | Severity |
|------|-----------|-------------------|----------|
| Cyclic dependency | services call each other in a cycle over synchronous calls | any cycle | high |
| Shared database | several services use one database directly | 2 services | high |
| Wide blast radius | the failure of a node would impair more than a share of the services | above 50 %, at least 3 services | high |
| Cross-domain coupling | a service calls synchronously into too many other domains | more than 3 domains | medium |
| Fan-in hub | a service has too many direct callers | more than 8 callers | medium |
| Long synchronous chain | a request path has too many synchronous hops | more than 5 hops | medium |
| Unknown external | an external host is neither on the allowlist nor a configured platform host | empty allowlist | low |
| Data stream without producer | a topic is consumed but nobody publishes to it in the traces (an outbox filled by change data capture looks like this) | none | low |

Self-calls and paths that close through a data stream are never cycles: asynchronous
decoupling is exactly what the rules give credit for. A cycle counts as one hop of a chain
and has its own finding. Platform services every workload talks to (a feature-flag or
configuration server) belong in `architrace.topology.platform-hosts` rather than in the
allowlist: the control plane labels them `category=platform` in every graph, so the rule
treats them as known and the map can group them. The thresholds are on the
[configuration](../../reference/configuration/#control-plane) page.

## What breaks if it fails

The map answers the blast radius of any node, not only of those that crossed a threshold.
Select a node and choose the **Impact** lens (or press **What breaks if it fails** in the
context rail; the lens is `lens=impact` in the URL). The map keeps only the nodes the failure
reaches: **impaired** nodes, which fail or error because they call the subject synchronously,
directly or through other impaired nodes, are coloured red, strong one hop away and lighter
further away; **delayed** nodes, which keep running but receive their data late because they
consume a stream the failure starves, are hatched. The rail card **If X fails** states how
many services out of all are impaired and lists every reached node by distance with its path
to the subject; clicking a row asks the same question about that node.

A data store impairs its clients and their callers; a data stream impairs its producers (a
publish is on the request path) and delays its consumers. **Show on map** of a wide blast
radius finding opens this lens on its subject.

## From a script

```bash
curl -s 'http://localhost:8085/api/v1/scopes/webshop/PROD/k8s-prod-eu1/findings?severity=HIGH' | jq .
curl -s 'http://localhost:8085/api/v1/scopes/webshop/PROD/k8s-prod-eu1/impact?node=db:postgresql/orders' | jq .impaired
```

The response shapes and the error types are in the
[Query API reference](../../reference/query-api/#findings-and-blast-radius).
