import { useMemo } from "react";
import {
  Background,
  Controls,
  MarkerType,
  MiniMap,
  Position,
  ReactFlow,
  Handle,
  type NodeProps,
  type Edge,
  type Node,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";
import type { DependencyGraphView } from "../api";

type GraphNodeData = {
  label: string;
  packageName: string;
  entryPoint: boolean;
};

function ModuleNode({ data }: NodeProps<{ label: string; packageName: string; entryPoint: boolean }>) {
  return (
    <div className={`graph-node ${data.entryPoint ? "graph-node-entry" : ""}`}>
      <Handle type="target" position={Position.Left} className="graph-handle" />
      <div className="graph-node-label">{data.label}</div>
      <div className="graph-node-package">{data.packageName}</div>
      {data.entryPoint && <span className="graph-node-badge">Entry point</span>}
      <Handle type="source" position={Position.Right} className="graph-handle" />
    </div>
  );
}

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

    const nodes: Node<GraphNodeData>[] = graph.nodes.map((n) => {
      const d = depth.get(n.id) ?? 0;
      const idx = perLevel.get(d) ?? 0;
      perLevel.set(d, idx + 1);
      return {
        id: n.id,
        type: "moduleNode",
        position: { x: d * 250 + 20, y: idx * 110 + 20 },
        data: { label: n.label, packageName: n.packageName, entryPoint: n.entryPoint },
        sourcePosition: Position.Right,
        targetPosition: Position.Left,
      };
    });

    const edges: Edge[] = graph.edges.map((e, i) => ({
      id: `e${i}`,
      source: e.source,
      target: e.target,
      markerEnd: { type: MarkerType.ArrowClosed },
      style: { stroke: "rgba(148, 163, 184, 0.42)" },
    }));

    return { nodes, edges };
  }, [graph]);

  if (graph.nodes.length === 0) {
    return (
      <div className="graph-empty">
        <strong>No graph data yet.</strong>
        <p>Run an assessment to visualize the dependency flow.</p>
      </div>
    );
  }

  return (
    <div className="graph">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        fitView
        minZoom={0.2}
        nodeTypes={{ moduleNode: ModuleNode }}
        proOptions={{ hideAttribution: true }}
      >
        <Background color="rgba(148, 163, 184, 0.16)" gap={22} />
        <MiniMap pannable zoomable nodeStrokeColor="#394150" nodeColor="#11161d" />
        <Controls showInteractive={false} />
      </ReactFlow>
    </div>
  );
}
