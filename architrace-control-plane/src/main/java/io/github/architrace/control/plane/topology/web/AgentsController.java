/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.web;

import io.github.architrace.control.plane.api.AgentsApi;
import io.github.architrace.control.plane.api.model.AgentDto;
import io.github.architrace.control.plane.topology.TopologyQuery;
import io.github.architrace.control.plane.web.ApiPaths;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE)
class AgentsController implements AgentsApi {

  private final TopologyQuery query;

  AgentsController(TopologyQuery query) {
    this.query = query;
  }

  @Override
  public List<AgentDto> listAgents() {
    return query.agents().stream().map(ApiModels::toDto).toList();
  }
}
