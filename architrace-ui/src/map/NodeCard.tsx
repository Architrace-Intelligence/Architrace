/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { Handle, type Node, type NodeProps, Position } from "@xyflow/react";
import type { NodeType, TopologyNode } from "../api/client";
import { NODE_TYPE_LABELS, NODE_TYPE_TOKENS, subtitle } from "./model";

export type CardNode = Node<{ readonly node: TopologyNode }, "card">;

const ICONS: Record<NodeType, string> = {
  SERVICE: "M4 6.5h16v4.5H4zM4 13h16v4.5H4zM7.5 8.75h.01M7.5 15.25h.01",
  DATABASE:
    "M4 6c0-1.7 3.6-3 8-3s8 1.3 8 3-3.6 3-8 3-8-1.3-8-3zM4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3",
  TOPIC: "M4 11a9 9 0 0 1 9 9M4 4a16 16 0 0 1 16 16M5 19h.01",
  EXTERNAL: "M12 3a9 9 0 1 0 0 18 9 9 0 1 0 0-18zM3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18",
};

export function NodeCard({ data }: NodeProps<CardNode>) {
  const { node } = data;
  return (
    <div className="node" title={node.id}>
      <Handle type="target" position={Position.Left} isConnectable={false} />
      <span className={`node-icon t-${NODE_TYPE_TOKENS[node.type]}`}>
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d={ICONS[node.type]} />
        </svg>
        <span className="sr-only">{NODE_TYPE_LABELS[node.type].singular}</span>
      </span>
      <span className="node-text">
        <span className="node-name">{node.name}</span>
        <span className="node-sub mono">{subtitle(node)}</span>
      </span>
      <Handle type="source" position={Position.Right} isConnectable={false} />
    </div>
  );
}
