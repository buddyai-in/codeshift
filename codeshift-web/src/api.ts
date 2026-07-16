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

// --- Projects + persistence + new-code addition ----------------------------

export interface ProjectSummary {
  id: string;
  name: string;
  sourceLanguage: string | null;
  targetStack: string | null;
  status: string;
}
export interface VersionSummary {
  versionId: string;
  versionNumber: number;
  approved: boolean;
  approvedBy: string | null;
  nodeCount: number;
}
export interface FeatureResponse {
  versionId: string;
  versionNumber: number;
  bsg: BsgGraph;
}

export async function createProject(
  name: string,
  sourceLanguage: string,
  targetStack: string,
): Promise<{ projectId: string }> {
  return json(
    await fetch("/projects", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, sourceLanguage, targetStack }),
    }),
  );
}

export async function listProjects(): Promise<ProjectSummary[]> {
  return json(await fetch("/projects"));
}

export async function getBsgVersions(projectId: string): Promise<VersionSummary[]> {
  return json(await fetch(`/projects/${projectId}/bsg/versions`));
}

export interface DebtReport {
  debtScore: number;
  grade: string;
  signals: string[];
  delta: { addedRefs: string[]; removedRefs: string[]; unchanged: number };
}

export async function getDebt(projectId: string): Promise<DebtReport> {
  return json(await fetch(`/projects/${projectId}/debt`));
}

export interface ProjectHealth {
  projectId: string;
  name: string;
  versionCount: number;
  nodeCount: number;
  debtScore: number;
  debtGrade: string;
}
export interface PortfolioReport {
  projectCount: number;
  avgDebtScore: number;
  projects: ProjectHealth[];
}

export async function getPortfolio(): Promise<PortfolioReport> {
  return json(await fetch("/portfolio"));
}

/** Seed an initial BSG so features can be added on top (demo helper). */
export async function seedBsg(projectId: string, bsg: BsgGraph): Promise<{ versionNumber: number }> {
  return json(
    await fetch(`/projects/${projectId}/bsg`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(bsg),
    }),
  );
}

export type NewCodeMode = "FEATURE" | "INTEGRATION" | "ARCHITECTURE" | "GREENFIELD";

export async function addFeature(
  projectId: string,
  request: string,
  mode: NewCodeMode = "FEATURE",
): Promise<FeatureResponse> {
  return json(
    await fetch(`/projects/${projectId}/feature-requests`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ request, mode }),
    }),
  );
}

// --- DataShift (Oracle -> PostgreSQL DDL) ----------------------------------

export interface TypeMapping {
  source: string;
  target: string;
  occurrences: number;
}
export interface DataShiftResult {
  sourceDialect: string;
  targetDialect: string;
  convertedDdl: string;
  mappings: TypeMapping[];
  warnings: string[];
}

export async function convertDdl(ddl: string): Promise<DataShiftResult> {
  return json(
    await fetch("/datashift/convert", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ddl }),
    }),
  );
}
