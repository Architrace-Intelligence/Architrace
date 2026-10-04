/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { describe, expect, it } from "vitest";
import type { NodeChange, Scope, TopologyDiff } from "../api/client";
import { demoDiff, demoGraph, demoScopes, emptyDiff } from "../test/http";
import {
  counters,
  defaultFrom,
  defaultLeft,
  describeChange,
  describeSelected,
  diffGroups,
  type DriftState,
  headline,
  INITIAL_DRIFT_STATE,
  isReady,
  nameOf,
  otherScopes,
  overlay,
  parseDriftState,
  sideLabels,
  summarise,
  toDriftParams,
  totalDifferences,
  unionGraph,
} from "./model";

const PROD: Scope = { project: "webshop", environment: "PROD", cluster: "k8s-prod-eu1" };
const SIDES = { left: "DEV · k8s-dev-ci", right: "PROD · k8s-prod-eu1" };

describe("drift state", () => {
  it("reads both modes from the URL and writes back only what differs from the defaults", () => {
    expect(parseDriftState(new URLSearchParams(""))).toEqual(INITIAL_DRIFT_STATE);
    const environments = parseDriftState(
      new URLSearchParams(
        "left=DEV&leftCluster=k8s-dev&at=2026-10-01T12:00:00Z&view=map&node=service:orders",
      ),
    );
    expect(environments).toEqual({
      mode: "environments",
      left: { environment: "DEV", cluster: "k8s-dev" },
      at: "2026-10-01T12:00:00Z",
      from: undefined,
      to: undefined,
      view: "map",
      node: "service:orders",
    });
    expect(toDriftParams(environments).toString()).toBe(
      "left=DEV&leftCluster=k8s-dev&at=2026-10-01T12%3A00%3A00Z&view=map&node=service%3Aorders",
    );
    const timeline = parseDriftState(
      new URLSearchParams(
        "mode=timeline&from=2026-09-30T18:00:00Z&to=2026-10-01T12:00:00Z&left=DEV&leftCluster=x",
      ),
    );
    expect(timeline.mode).toBe("timeline");
    expect(timeline.from).toBe("2026-09-30T18:00:00Z");
    expect(timeline.to).toBe("2026-10-01T12:00:00Z");
    expect(toDriftParams(timeline).toString()).toBe(
      "mode=timeline&from=2026-09-30T18%3A00%3A00Z&to=2026-10-01T12%3A00%3A00Z",
    );
    expect(toDriftParams(INITIAL_DRIFT_STATE).toString()).toBe("");
  });

  it("ignores half a left side, unreadable instants and empty values", () => {
    const state = parseDriftState(
      new URLSearchParams("left=DEV&at=yesterday&from=&node=&view=table"),
    );
    expect(state.left).toBeUndefined();
    expect(state.at).toBeUndefined();
    expect(state.from).toBeUndefined();
    expect(state.node).toBeUndefined();
    expect(state.view).toBe("list");
    expect(isReady(state)).toBe(false);
    expect(isReady({ ...state, left: { environment: "DEV", cluster: "k8s-dev" } })).toBe(true);
    expect(isReady({ ...state, mode: "timeline" })).toBe(false);
    expect(isReady({ ...state, mode: "timeline", from: "2026-09-30T18:00:00Z" })).toBe(true);
  });

  it("offers the other scopes of the project and prefers another environment as the default left side", () => {
    const scopes = demoScopes.map((summary) => summary.scope);
    const others = otherScopes(scopes, PROD);
    expect(others.map((scope) => `${scope.environment}/${scope.cluster}`)).toEqual([
      "DEV/k8s-dev-ci",
      "DEV/k8s-dev-eu1",
      "PROD/k8s-prod-eu2",
    ]);
    expect(defaultLeft(others, PROD)).toEqual({ environment: "DEV", cluster: "k8s-dev-ci" });
    const prodOnly = others.filter((scope) => scope.environment === "PROD");
    expect(defaultLeft(prodOnly, PROD)).toEqual({ environment: "PROD", cluster: "k8s-prod-eu2" });
    expect(defaultLeft([], PROD)).toBeUndefined();
  });

  it("defaults the timeline to one day before, whole seconds", () => {
    expect(defaultFrom(new Date("2026-10-01T12:00:00.750Z"))).toBe("2026-09-30T12:00:00.000Z");
  });

  it("labels the sides per mode", () => {
    const base: DriftState = {
      ...INITIAL_DRIFT_STATE,
      left: { environment: "DEV", cluster: "k8s-dev-ci" },
    };
    expect(sideLabels(PROD, base)).toEqual(SIDES);
    expect(sideLabels(PROD, INITIAL_DRIFT_STATE)).toBeUndefined();
    expect(sideLabels(PROD, { ...INITIAL_DRIFT_STATE, mode: "timeline" })).toBeUndefined();
    expect(
      sideLabels(PROD, { ...INITIAL_DRIFT_STATE, mode: "timeline", from: "2026-09-30T18:00:00Z" }),
    ).toEqual({ left: "PROD · 2026-09-30 18:00:00 UTC", right: "PROD · now" });
    expect(
      sideLabels(PROD, {
        ...INITIAL_DRIFT_STATE,
        mode: "timeline",
        from: "2026-09-30T18:00:00Z",
        to: "2026-10-01T12:00:00Z",
      })?.right,
    ).toBe("PROD · 2026-10-01 12:00:00 UTC");
  });
});

describe("diff views", () => {
  it("counts every kind of difference", () => {
    expect(counters(demoDiff)).toEqual({
      nodesAdded: 2,
      nodesRemoved: 2,
      nodesChanged: 1,
      edgesAdded: 2,
      edgesRemoved: 2,
    });
    expect(totalDifferences(demoDiff)).toBe(9);
    expect(totalDifferences(emptyDiff)).toBe(0);
    expect(headline(demoDiff)).toBe("9 differences · 2 + 2 nodes, 1 changed, 4 dependencies");
    expect(headline(emptyDiff)).toBe("No differences");
  });

  it("groups the rows by node type and dependencies with a one-line summary each", () => {
    const groups = diffGroups(demoDiff, SIDES);
    expect(groups.map((group) => `${group.label}: ${group.summary}`)).toEqual([
      "Services: 1 added · 1 removed · 1 changed",
      "Data stores: 0 added · 1 removed · 0 changed",
      "Data streams: 1 added · 0 removed · 0 changed",
      "External: identical",
      "Dependencies: 2 added · 2 removed · 0 changed",
    ]);
    const services = groups[0]?.rows ?? [];
    expect(services.map((row) => [row.kind, row.name, row.detail])).toEqual([
      ["added", "notification-service", "exists only in PROD · k8s-prod-eu1"],
      ["removed", "search-service", "exists only in DEV · k8s-dev-ci · v2.3.0"],
      ["changed", "orders-service", "v2.9.0 in DEV · k8s-dev-ci → v2.8.1 in PROD · k8s-prod-eu1"],
    ]);
    const dependencies = groups[4]?.rows ?? [];
    expect(dependencies.map((row) => [row.kind, row.name, row.detail, row.nodeId])).toEqual([
      [
        "added",
        "payments-service → payment-events",
        "publish · only in PROD · k8s-prod-eu1",
        "topic:kafka/payment-events",
      ],
      [
        "added",
        "payment-events → notification-service",
        "consume · only in PROD · k8s-prod-eu1",
        "service:notification-service",
      ],
      [
        "removed",
        "search-service → postgresql/catalog",
        "sync · only in DEV · k8s-dev-ci",
        "db:postgresql/catalog",
      ],
      [
        "removed",
        "search-service → postgresql/orders",
        "sync · only in DEV · k8s-dev-ci",
        "db:postgresql/orders",
      ],
    ]);
  });

  it("describes a deployment-only change and names unknown nodes by their id", () => {
    const moved: NodeChange = {
      id: "service:orders",
      type: "SERVICE",
      name: "orders",
      versionsBefore: ["1.0"],
      versionsAfter: ["1.0"],
      deploymentsBefore: [{ cluster: "k8s-prod-eu1", namespace: "orders" }],
      deploymentsAfter: [{ cluster: "k8s-prod-eu1", namespace: "" }],
    };
    expect(describeChange(moved, SIDES)).toBe("deployed at k8s-prod-eu1/orders → k8s-prod-eu1");
    expect(describeChange({ ...moved, deploymentsAfter: [] }, SIDES)).toBe(
      "deployed at k8s-prod-eu1/orders → nowhere",
    );
    expect(describeChange({ ...moved, versionsBefore: [] }, SIDES)).toBe(
      "no version in DEV · k8s-dev-ci → v1.0 in PROD · k8s-prod-eu1",
    );
    expect(nameOf("db:postgresql/orders", new Map())).toBe("postgresql/orders");
    expect(nameOf("service:x", new Map([["service:x", "x-service"]]))).toBe("x-service");
  });

  it("turns the diff into map overlays and a union graph with ghost nodes and edges", () => {
    const marks = overlay(demoDiff);
    expect(marks.nodes.get("service:notification-service")).toBe("added");
    expect(marks.nodes.get("service:search-service")).toBe("removed");
    expect(marks.nodes.get("service:orders-service")).toBe("changed");
    expect(marks.subtitles.get("service:orders-service")).toBe("v2.9.0 → v2.8.1");
    expect(marks.edges.get("service:payments-service>topic:kafka/payment-events:PUBLISH")).toBe(
      "added",
    );
    expect(marks.edges.get("service:search-service>db:postgresql/catalog:SYNC")).toBe("removed");

    const union = unionGraph(demoGraph, demoDiff);
    expect(union.nodes).toHaveLength(demoGraph.nodes.length + 2);
    expect(union.nodes.map((node) => node.id)).toContain("service:search-service");
    expect(union.edges).toHaveLength(demoGraph.edges.length + 2);
    const ghost = union.edges.find((edge) => edge.sourceId === "service:search-service");
    expect(ghost?.metrics.calls).toBe(0);
    expect(unionGraph(demoGraph, emptyDiff)).toEqual({
      nodes: demoGraph.nodes,
      edges: demoGraph.edges,
    });

    const alreadyThere: TopologyDiff = {
      ...emptyDiff,
      nodesRemoved: demoGraph.nodes.slice(0, 1),
      edgesRemoved: demoGraph.edges.slice(0, 1),
    };
    const same = unionGraph(demoGraph, alreadyThere);
    expect(same.nodes).toHaveLength(demoGraph.nodes.length);
    expect(same.edges).toHaveLength(demoGraph.edges.length);
  });

  it("writes deterministic sentences for every kind of drift and for aligned sides", () => {
    const sentences = summarise(demoDiff, SIDES);
    expect(sentences.map((sentence) => [sentence.badge, sentence.title])).toEqual([
      ["+2", "notification-service, payment-events exist only in PROD · k8s-prod-eu1"],
      ["−2", "postgresql/catalog, search-service missing from PROD · k8s-prod-eu1"],
      ["Δ1", "1 node differs between the sides"],
      ["4", "4 dependencies differ"],
    ]);
    expect(sentences[0]?.body).toBe(
      "Brings payments-service → payment-events, payment-events → notification-service.",
    );
    expect(sentences[2]?.body).toBe(
      "orders-service v2.9.0 in DEV · k8s-dev-ci → v2.8.1 in PROD · k8s-prod-eu1.",
    );
    expect(sentences[3]?.body).toContain(
      "search-service → postgresql/orders only in DEV · k8s-dev-ci",
    );

    const lonely: TopologyDiff = { ...emptyDiff, nodesAdded: demoDiff.nodesAdded.slice(1, 2) };
    const added = summarise(lonely, SIDES)[0];
    expect(added?.title).toBe("payment-events exists only in PROD · k8s-prod-eu1");
    expect(added?.body).toBe("No dependency touches them yet.");

    const removedEdge: TopologyDiff = {
      ...emptyDiff,
      edgesRemoved: demoDiff.edgesRemoved.slice(0, 1),
    };
    expect(summarise(removedEdge, SIDES)[0]).toMatchObject({
      kind: "removed",
      badge: "1",
      title: "1 dependency differs",
    });

    expect(summarise(emptyDiff, SIDES)).toEqual([
      {
        kind: "aligned",
        badge: "aligned",
        title: "Both sides are identical",
        body: "Same node ids, versions and dependency keys. Metrics may still differ; they are not part of drift.",
      },
    ]);
  });

  it("describes the selected node from the diff, or from the graph when it is the same on both sides", () => {
    expect(describeSelected(demoDiff, "service:notification-service", SIDES, undefined)).toEqual({
      id: "service:notification-service",
      name: "notification-service",
      kind: "added",
      detail: "Exists only in PROD · k8s-prod-eu1.",
    });
    expect(describeSelected(demoDiff, "service:search-service", SIDES, undefined)?.detail).toBe(
      "Exists only in DEV · k8s-dev-ci.",
    );
    expect(describeSelected(demoDiff, "service:orders-service", SIDES, undefined)).toMatchObject({
      kind: "changed",
      detail: "v2.9.0 in DEV · k8s-dev-ci → v2.8.1 in PROD · k8s-prod-eu1",
    });
    expect(describeSelected(demoDiff, "service:api-gateway", SIDES, demoGraph)).toEqual({
      id: "service:api-gateway",
      name: "api-gateway",
      kind: "same",
      detail: "Identical on both sides.",
    });
    expect(describeSelected(demoDiff, "service:api-gateway", SIDES, undefined)).toBeUndefined();
  });
});
