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

// --- Migration runs + BSG review -------------------------------------------

export type NodeType =
  | "BUSINESS_RULE" | "DATA_FLOW" | "STATE_TRANSITION" | "EXTERNAL_CONTRACT"
  | "EDGE_CASE" | "IMPLICIT_RULE" | "MESSAGING_CONTRACT";
export type Confidence = "HIGH" | "MEDIUM" | "LOW";
export type HumanStatus = "PENDING" | "APPROVED" | "REJECTED" | "MODIFIED";

export interface BsgNode {
  nodeRef: string;
  nodeType: NodeType;
  title: string;
  description: string;
  sourceLocation: string | null;
  confidence: Confidence;
  humanStatus: HumanStatus;
  origin: string;
  targetCodeLocation: string | null;
  testCoverage: boolean;
}

export interface BsgGraph {
  projectId: string;
  versionNumber: number;
  nodes: BsgNode[];
  edges: unknown[];
}

export interface RunStart {
  threadId: string;
  phase: string;
  awaitingHuman: boolean;
  translationOrder: string[];
  bsgNodeCount: number;
  log: string[];
}

export interface ResumeResult {
  threadId: string;
  phase: string;
  reviewDecision: string;
  log: string[];
}

async function json<T>(res: Response): Promise<T> {
  if (!res.ok) {
    throw new Error(`Request failed (${res.status}). ${await res.text().catch(() => "")}`);
  }
  return res.json();
}

/** Start a migration run from an uploaded source zip (runs discovery + analysis). */
export async function startRunUpload(file: File, projectName: string): Promise<RunStart> {
  const form = new FormData();
  form.append("file", file);
  if (projectName) form.append("projectName", projectName);
  return json(await fetch("/runs/upload", { method: "POST", body: form }));
}

/** Start a migration run from a server-accessible source directory (sample demo). */
export async function startRunPath(projectPath: string, projectId: string): Promise<RunStart> {
  return json(
    await fetch("/runs", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ projectId, projectPath }),
    }),
  );
}

export async function getBsg(threadId: string): Promise<BsgGraph> {
  return json(await fetch(`/runs/${threadId}/bsg`));
}

/** Record a review decision + optional edits on one BSG node. */
export async function reviewNode(
  threadId: string,
  nodeRef: string,
  patch: { status?: HumanStatus; title?: string; description?: string },
): Promise<BsgGraph> {
  return json(
    await fetch(`/runs/${threadId}/bsg/nodes/${encodeURIComponent(nodeRef)}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(patch),
    }),
  );
}

export async function resumeRun(threadId: string, decision: string): Promise<ResumeResult> {
  return json(
    await fetch(`/runs/${threadId}/resume`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ decision }),
    }),
  );
}

// --- Architecture plan (gate #2) -------------------------------------------

export interface ModuleMapping {
  moduleId: string;
  targetClass: string;
  layer: string;
}
export interface ServiceBoundary {
  name: string;
  moduleIds: string[];
}
export interface MigrationPhase {
  order: number;
  name: string;
  moduleIds: string[];
}
export interface ArchitecturePlan {
  targetStack: string;
  moduleMappings: ModuleMapping[];
  microservices: ServiceBoundary[];
  phases: MigrationPhase[];
}

export async function getArchitecture(threadId: string): Promise<ArchitecturePlan> {
  return json(await fetch(`/runs/${threadId}/architecture`));
}

// --- Transformation (build) ------------------------------------------------

export interface TransformedModule {
  moduleId: string;
  targetClass: string;
  layer: string;
  sourceCode: string;
  compiled: boolean;
  bsgRuleRefs: string[];
}
export interface GeneratedTest {
  testClass: string;
  sourceCode: string;
  bsgRuleRef: string;
}
export interface TransformationResult {
  modules: TransformedModule[];
  tests: GeneratedTest[];
  allCompiled: boolean;
  diagnostics: string[];
}

export async function getTransformation(threadId: string): Promise<TransformationResult> {
  return json(await fetch(`/runs/${threadId}/transformation`));
}

export interface ValidationReport {
  compileOk: boolean;
  bsgNodeCount: number;
  coveredNodeCount: number;
  coveragePercent: number;
  passed: boolean;
  issues: string[];
}

export async function getValidation(threadId: string): Promise<ValidationReport> {
  return json(await fetch(`/runs/${threadId}/validation`));
}

// --- Hardening (security + devops + messaging) -----------------------------

export interface SecurityFinding {
  severity: string;
  message: string;
  location: string;
}
export interface TopicProposal {
  name: string;
  partitions: number;
  partitionKey: string;
  consumerGroup: string;
}
export interface HardeningResult {
  security: { findings: SecurityFinding[]; highCount: number };
  devops: { dockerfile: string; kubernetesManifest: string; ciPipeline: string };
  messaging: { sourceSystems: string[]; topics: TopicProposal[] };
}

export async function getHardening(threadId: string): Promise<HardeningResult> {
  return json(await fetch(`/runs/${threadId}/hardening`));
}
