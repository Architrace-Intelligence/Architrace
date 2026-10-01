---
title: 0004. React and TypeScript single-page UI served by the control plane
description: The web UI is a React + TypeScript application bundled into the control plane artifact and talking to the Query API only.
---

Status: accepted
Date: 2026-10-01

## Context

The service map, drift views and findings (requirements F8, F9, F10) need an interactive graph
with filtering, selection, detail panels and side-by-side comparison. The maintainer wants a
single deployable artifact and a stack that is easy to hire for and to contribute to. Two
approaches were on the table: a browser application with a rich graph library, or server-side
rendering inside Spring Boot.

## Decision

- The UI is a single-page application written in **React + TypeScript**, kept in its own
  module of the monorepo and built with Vite.
- The production bundle is packaged into the control plane jar as static resources and served
  from the same HTTP port as the API, so one artifact ships both.
- The UI consumes only the public Query API (REST + OpenAPI). The TypeScript client is
  generated from the OpenAPI document; no other coupling between Java and TypeScript exists.
- The Node toolchain is driven from Gradle so `./gradlew build` produces the full artifact,
  and the frontend has its own quality gates (TypeScript strict mode, ESLint, Prettier, unit
  tests) in the PR pipeline.
- The graph rendering library is chosen in the service map feature design.

## Consequences

- CI needs Node in addition to the JDK; build time grows accordingly.
- The control plane gains a static-resources route and SPA fallback; it does not gain any
  server-side templating.
- The API is a contract with an external consumer from day one: breaking changes need
  versioning.
- A separately hosted frontend (CDN, nginx) remains possible later because the bundle is
  plain static files.

## Alternatives considered

- **Server-side rendering (Thymeleaf, htmx)**: simplest build, rejected because interactive
  graph exploration and diff views would be poor.
- **Separate frontend deployment**: cleaner separation, rejected for the MVP because it doubles
  the artifacts and the deployment steps.
- **Java-driven UI frameworks (Vaadin and similar)**: rejected for weaker ecosystem fit with
  graph visualisation and smaller contributor pool.
