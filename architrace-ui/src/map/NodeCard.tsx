/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { Handle, type Node, type NodeProps, Position } from "@xyflow/react";
import type { TopologyNode } from "../api/client";
import { subtitle } from "./model";
import { NodeIcon } from "./NodeIcon";

export type CardNode = Node<{ readonly node: TopologyNode }, "card">;

export function NodeCard({ data }: NodeProps<CardNode>) {
  const { node } = data;
  return (
    <div className="node" title={node.id}>
      <Handle type="target" position={Position.Left} isConnectable={false} />
      <NodeIcon type={node.type} />
      <span className="node-text">
        <span className="node-name">{node.name}</span>
        <span className="node-sub mono">{subtitle(node)}</span>
      </span>
      <Handle type="source" position={Position.Right} isConnectable={false} />
    </div>
  );
}
