/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { describe, expect, it } from "vitest";
import { demoScopes } from "../test/http";
import { scopeOptions, switchScope } from "./switcher";

const scopes = demoScopes.map((summary) => summary.scope);
const current = { project: "webshop", environment: "PROD", cluster: "k8s-prod-eu1" };

describe("scope switcher", () => {
  it("offers the values that exist above and beside the current scope", () => {
    expect(scopeOptions(scopes, current, "project")).toEqual(["billing", "webshop"]);
    expect(scopeOptions(scopes, current, "environment")).toEqual(["DEV", "PROD"]);
    expect(scopeOptions(scopes, current, "cluster")).toEqual(["k8s-prod-eu1", "k8s-prod-eu2"]);
    expect(scopeOptions(scopes, { ...current, project: "ghost" }, "environment")).toEqual([]);
  });

  it("switches the project to the scope that shares the most with the current one", () => {
    expect(switchScope(scopes, current, "project", "billing")).toEqual({
      project: "billing",
      environment: "PROD",
      cluster: "k8s-prod-eu1",
    });
    expect(
      switchScope(scopes, { ...current, cluster: "k8s-prod-eu2" }, "project", "billing"),
    ).toEqual({ project: "billing", environment: "PROD", cluster: "k8s-prod-eu1" });
    expect(
      switchScope(
        scopes,
        { project: "billing", environment: "STAGE", cluster: "k8s-stage-eu1" },
        "project",
        "webshop",
      ),
    ).toEqual({ project: "webshop", environment: "DEV", cluster: "k8s-dev-ci" });
  });

  it("switches the environment keeping the cluster when it exists, and the cluster directly", () => {
    expect(switchScope(scopes, current, "environment", "DEV")).toEqual({
      project: "webshop",
      environment: "DEV",
      cluster: "k8s-dev-ci",
    });
    expect(switchScope(scopes, current, "cluster", "k8s-prod-eu2")).toEqual({
      project: "webshop",
      environment: "PROD",
      cluster: "k8s-prod-eu2",
    });
    expect(switchScope(scopes, current, "cluster", "nowhere")).toBeUndefined();
  });
});
