import { useMemo } from "react";
import {
  Background,
  Controls,
  MarkerType,
  MiniMap,
  Position,
  ReactFlow,
  type Edge,
  type Node,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";
import type { DependencyGraphView } from "../api";

// Longest dependency chain from each node → a left-to-right layered layout
// (leaves on the left, entry points on the right).
function computeDepths(graph: DependencyGraphView): Map<string, number> {
  const deps = new Map<string, string[]>();
  graph.nodes.forEach((n) => deps.set(n.id, []));
  graph.edges.forEach((e) => deps.get(e.source)?.push(e.target));

  const depth = new Map<string, number>();
  const visiting = new Set<string>();
  const walk = (id: string): number => {
    if (depth.has(id)) return depth.get(id)!;
    if (visiting.has(id)) return 0; // cycle guard
    visiting.add(id);
    const d = (deps.get(id) ?? []).reduce((max, t) => Math.max(max, walk(t) + 1), 0);
    visiting.delete(id);
    depth.set(id, d);
    return d;
  };
  graph.nodes.forEach((n) => walk(n.id));
  return depth;
}

export default function DependencyGraph({ graph }: { graph: DependencyGraphView }) {
  const { nodes, edges } = useMemo(() => {
    const depth = computeDepths(graph);
    const perLevel = new Map<number, number>();

    const nodes: Node[] = graph.nodes.map((n) => {
      const d = depth.get(n.id) ?? 0;
      const idx = perLevel.get(d) ?? 0;
      perLevel.set(d, idx + 1);
      return {
        id: n.id,
        position: { x: d * 240 + 40, y: idx * 96 + 40 },
        data: { label: n.label },
        sourcePosition: Position.Right,
        targetPosition: Position.Left,
        style: {
          border: n.entryPoint ? "2px solid #6d5cff" : "1px solid #33384a",
          background: n.entryPoint ? "#f3f1ff" : "#ffffff",
          borderRadius: 10,
          padding: "8px 12px",
          fontSize: 12,
          fontWeight: 600,
          color: "#1c2030",
        },
      };
    });

    const edges: Edge[] = graph.edges.map((e, i) => ({
      id: `e${i}`,
      source: e.source,
      target: e.target,
      markerEnd: { type: MarkerType.ArrowClosed },
      style: { stroke: "#9aa0b4" },
    }));

    return { nodes, edges };
  }, [graph]);

  return (
    <div className="graph">
      <ReactFlow nodes={nodes} edges={edges} fitView minZoom={0.2} proOptions={{ hideAttribution: true }}>
        <Background color="#e6e8f0" gap={20} />
        <MiniMap pannable zoomable />
        <Controls showInteractive={false} />
      </ReactFlow>
    </div>
  );
}
