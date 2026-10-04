/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.drift.web;

import io.github.architrace.control.plane.api.model.EdgeKindDto;
import io.github.architrace.control.plane.api.model.EdgeRefDto;
import io.github.architrace.control.plane.api.model.GraphRefDto;
import io.github.architrace.control.plane.api.model.NodeChangeDto;
import io.github.architrace.control.plane.api.model.NodeTypeDto;
import io.github.architrace.control.plane.api.model.TopologyDiffDto;
import io.github.architrace.control.plane.drift.GraphRef;
import io.github.architrace.control.plane.drift.NodeChange;
import io.github.architrace.control.plane.drift.TopologyDiff;
import io.github.architrace.control.plane.topology.EdgeKey;
import io.github.architrace.control.plane.topology.NodeAttributes;
import io.github.architrace.control.plane.topology.TopologyNode;
import io.github.architrace.control.plane.topology.web.ApiModels;

final class DriftModels {

    private DriftModels() {}

    static TopologyDiffDto toDto(TopologyDiff diff) {
        return new TopologyDiffDto(
                toDto(diff.left()),
                toDto(diff.right()),
                diff.nodesAdded().stream().map(ApiModels::toDto).toList(),
                diff.nodesRemoved().stream().map(ApiModels::toDto).toList(),
                diff.nodesChanged().stream().map(DriftModels::toDto).toList(),
                diff.edgesAdded().stream().map(DriftModels::toDto).toList(),
                diff.edgesRemoved().stream().map(DriftModels::toDto).toList());
    }

    static GraphRefDto toDto(GraphRef ref) {
        return new GraphRefDto(ApiModels.toDto(ref.scope()), ApiModels.atUtc(ref.at()));
    }

    static NodeChangeDto toDto(NodeChange change) {
        TopologyNode node = change.after();
        NodeAttributes before = change.before().attributes();
        NodeAttributes after = node.attributes();
        return new NodeChangeDto(
                node.id(),
                NodeTypeDto.fromValue(node.type().name()),
                node.name(),
                ApiModels.versions(before),
                ApiModels.versions(after),
                ApiModels.deployments(before),
                ApiModels.deployments(after));
    }

    static EdgeRefDto toDto(EdgeKey edge) {
        return new EdgeRefDto(
                edge.sourceId(),
                edge.targetId(),
                EdgeKindDto.fromValue(edge.kind().name()));
    }
}
