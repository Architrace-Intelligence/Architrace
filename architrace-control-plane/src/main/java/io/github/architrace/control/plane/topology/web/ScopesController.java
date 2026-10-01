/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.web;

import io.github.architrace.control.plane.api.ScopesApi;
import io.github.architrace.control.plane.api.model.ScopeSummaryDto;
import io.github.architrace.control.plane.topology.TopologyQuery;
import io.github.architrace.control.plane.web.ApiPaths;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE)
class ScopesController implements ScopesApi {

  private final TopologyQuery query;

  ScopesController(TopologyQuery query) {
    this.query = query;
  }

  @Override
  public List<ScopeSummaryDto> listScopes() {
    return query.scopes().stream().map(ApiModels::toDto).toList();
  }
}
