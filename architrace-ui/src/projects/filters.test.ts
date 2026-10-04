/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { describe, expect, it } from "vitest";
import { demoScopes, scopeSummary } from "../test/http";
import {
  applyFilter,
  DEFAULT_GROUP_BY,
  EMPTY_FILTER,
  facetValues,
  groupScopes,
  isEmptyFilter,
  parseFilter,
  scopePath,
  summarise,
  toggleFacetValue,
  toParams,
} from "./filters";

describe("filters", () => {
  it("round-trips the filter through the URL and drops the defaults", () => {
    const filter = parseFilter(
      new URLSearchParams("env=PROD&env=DEV&env=PROD&cluster=k8s-prod-eu1&q=web&group=cluster"),
    );

    expect(filter).toEqual({
      environments: ["PROD", "DEV"],
      clusters: ["k8s-prod-eu1"],
      query: "web",
      groupBy: "cluster",
    });
    expect(toParams(filter).toString()).toBe(
      "env=PROD&env=DEV&cluster=k8s-prod-eu1&q=web&group=cluster",
    );
    expect(toParams(EMPTY_FILTER).toString()).toBe("");
  });

  it("falls back to the default grouping for an unknown value", () => {
    expect(parseFilter(new URLSearchParams("group=colour")).groupBy).toBe(DEFAULT_GROUP_BY);
    expect(isEmptyFilter(EMPTY_FILTER)).toBe(true);
    expect(isEmptyFilter({ ...EMPTY_FILTER, query: "x" })).toBe(false);
  });

  it("toggles facet values in and out", () => {
    const withProd = toggleFacetValue(EMPTY_FILTER, "environment", "PROD");
    const withCluster = toggleFacetValue(withProd, "cluster", "k8s-dev-ci");

    expect(withProd.environments).toEqual(["PROD"]);
    expect(withCluster.clusters).toEqual(["k8s-dev-ci"]);
    expect(toggleFacetValue(withCluster, "environment", "PROD").environments).toEqual([]);
    expect(toggleFacetValue(withCluster, "cluster", "k8s-dev-ci").clusters).toEqual([]);
  });

  it("applies environment, cluster and text filters together", () => {
    expect(applyFilter(demoScopes, EMPTY_FILTER)).toHaveLength(6);
    expect(applyFilter(demoScopes, { ...EMPTY_FILTER, environments: ["PROD"] })).toHaveLength(3);
    expect(
      applyFilter(demoScopes, {
        ...EMPTY_FILTER,
        environments: ["PROD"],
        clusters: ["k8s-prod-eu1"],
      }),
    ).toHaveLength(2);
    expect(applyFilter(demoScopes, { ...EMPTY_FILTER, query: " BILL " })).toHaveLength(2);
    expect(applyFilter(demoScopes, { ...EMPTY_FILTER, query: "eu2" })).toHaveLength(1);
    expect(applyFilter(demoScopes, { ...EMPTY_FILTER, query: "nothing" })).toHaveLength(0);
  });

  it("counts facet values with the other filters applied and keeps zero counts", () => {
    const filter = { ...EMPTY_FILTER, environments: ["PROD"], clusters: ["k8s-prod-eu1"] };

    expect(facetValues(demoScopes, filter, "cluster")).toEqual([
      { value: "k8s-dev-ci", count: 0, selected: false },
      { value: "k8s-dev-eu1", count: 0, selected: false },
      { value: "k8s-prod-eu1", count: 2, selected: true },
      { value: "k8s-prod-eu2", count: 1, selected: false },
      { value: "k8s-stage-eu1", count: 0, selected: false },
    ]);
    expect(facetValues(demoScopes, filter, "environment")).toEqual([
      { value: "DEV", count: 0, selected: false },
      { value: "PROD", count: 2, selected: true },
      { value: "STAGE", count: 0, selected: false },
    ]);
  });

  it("groups scopes by the chosen key with sorted keys and rows", () => {
    const byProject = groupScopes(demoScopes, "project");
    const byCluster = groupScopes(demoScopes, "cluster");

    expect(byProject.map((group) => group.key)).toEqual(["billing", "webshop"]);
    expect(byProject[1]).toMatchObject({
      projects: 1,
      environments: 2,
      clusters: 4,
      services: 18,
      dataStreams: 8,
    });
    expect(byProject[1]?.scopes.map((scope) => scope.scope.cluster)).toEqual([
      "k8s-dev-ci",
      "k8s-dev-eu1",
      "k8s-prod-eu1",
      "k8s-prod-eu2",
    ]);
    expect(byCluster.map((group) => group.key)).toEqual([
      "k8s-dev-ci",
      "k8s-dev-eu1",
      "k8s-prod-eu1",
      "k8s-prod-eu2",
      "k8s-stage-eu1",
    ]);
    expect(byCluster[2]).toMatchObject({ projects: 2, environments: 1, clusters: 1, services: 12 });
  });

  it("summarises the visible scopes against the total", () => {
    const visible = applyFilter(demoScopes, { ...EMPTY_FILTER, environments: ["DEV"] });

    expect(summarise(demoScopes, visible)).toEqual({
      total: 6,
      visible: 2,
      projects: 1,
      environments: 1,
      clusters: 2,
      agents: 2,
      staleAgents: 1,
    });
  });

  it("builds the scope path with encoded segments", () => {
    expect(scopePath(scopeSummary("web shop", "PROD", "k8s/eu1").scope)).toBe(
      "/scopes/web%20shop/PROD/k8s%2Feu1",
    );
  });
});
