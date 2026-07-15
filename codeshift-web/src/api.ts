// Types mirror the Java DTOs (AssessmentResult / AssessmentReport / DependencyGraphView).

export interface AssessmentReport {
  projectName: string;
  moduleCount: number;
  estimatedLoc: number;
  dependencyEdgeCount: number;
  entryPointCount: number;
  hasCycles: boolean;
  messagingSystems: string[];
  usesJavaxNamespace: boolean;
  migrationSignals: string[];
  complexityScore: number;
  estimatedEffortDays: number;
  priceEstimateUsd: number;
  suggestedTier: string;
  translationOrder: string[];
}

export interface GraphNode {
  id: string;
  label: string;
  packageName: string;
  entryPoint: boolean;
}

export interface GraphEdge {
  source: string;
  target: string;
}

export interface DependencyGraphView {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

export interface AssessmentResult {
  report: AssessmentReport;
  graph: DependencyGraphView;
}

async function handle(res: Response): Promise<AssessmentResult> {
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`Assessment failed (${res.status}). ${text}`);
  }
  return res.json();
}

/** Assess an uploaded source zip. */
export async function assessZip(file: File, projectName: string): Promise<AssessmentResult> {
  const form = new FormData();
  form.append("file", file);
  if (projectName) form.append("projectName", projectName);
  return handle(await fetch("/public/assess", { method: "POST", body: form }));
}

/** Assess a server-accessible source directory (used by the "Try sample" button). */
export async function assessPath(
  projectPath: string,
  projectName: string,
): Promise<AssessmentResult> {
  return handle(
    await fetch("/public/assess/path", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ projectPath, projectName }),
    }),
  );
}
