/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.web;

import java.util.List;

public final class ApiPaths {

  public static final String API = "/api";
  public static final String BASE = API + "/v1";
  public static final String DOCUMENT = BASE + "/openapi.yaml";
  public static final String SWAGGER_UI = "/swagger-ui";
  public static final String ACTUATOR = "/actuator";
  public static final String WEBJARS = "/webjars";
  public static final String UI_ASSETS = "/assets";
  public static final List<String> SERVER_ROUTES = List.of(API, ACTUATOR, SWAGGER_UI, WEBJARS);

  private ApiPaths() {}
}
