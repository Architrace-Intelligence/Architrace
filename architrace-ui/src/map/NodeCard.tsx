/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { Handle, type Node, type NodeProps, Position } from "@xyflow/react";
import type { TopologyNode } from "../api/client";
import { type Selection, subtitle } from "./model";
import { NodeIcon } from "./NodeIcon";

export type CardNode = Node<
  { readonly node: TopologyNode; readonly onSelect: (selection: Selection | undefined) => void },
  "card"
>;

export function NodeCard({ data, selected }: NodeProps<CardNode>) {
  const { node, onSelect } = data;
  return (
    <div
      className="node"
      title={node.id}
      role="button"
      tabIndex={0}
      aria-pressed={selected}
      onKeyDown={(event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          onSelect({ kind: "node", id: node.id });
        } else if (event.key === "Escape") {
          onSelect(undefined);
        }
      }}
    >
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
