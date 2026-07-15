import { useState } from "react";
import BsgReviewBoard from "../components/BsgReviewBoard";
import ArchitectureView from "../components/ArchitectureView";
import TransformationView from "../components/TransformationView";
import {
  getArchitecture,
  getBsg,
  getTransformation,
  getValidation,
  resumeRun,
  reviewNode,
  startRunPath,
  startRunUpload,
  type ArchitecturePlan,
  type BsgGraph,
  type HumanStatus,
  type RunStart,
  type TransformationResult,
  type ValidationReport,
} from "../api";

const SAMPLE_PATH = "codeshift-parser/src/test/resources/sample-project";

export default function MigratePage() {
  const [file, setFile] = useState<File | null>(null);
  const [name, setName] = useState("");
  const [run, setRun] = useState<RunStart | null>(null);
  const [bsg, setBsg] = useState<BsgGraph | null>(null);
  const [architecture, setArchitecture] = useState<ArchitecturePlan | null>(null);
  const [transformation, setTransformation] = useState<TransformationResult | null>(null);
  const [validation, setValidation] = useState<ValidationReport | null>(null);
  const [phase, setPhase] = useState<string>("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function begin(starter: () => Promise<RunStart>) {
    setError("");
    setBusy(true);
    try {
      const started = await starter();
      setRun(started);
      setPhase(started.phase);
      setBsg(await getBsg(started.threadId));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  async function onReview(nodeRef: string, patch: { status?: HumanStatus; title?: string; description?: string }) {
    if (!run) return;
    setBusy(true);
    try {
      setBsg(await reviewNode(run.threadId, nodeRef, patch));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  async function approveBsg() {
    if (!run) return;
    setBusy(true);
    try {
      const res = await resumeRun(run.threadId, "APPROVED");
      setPhase(res.phase);
      if (res.phase === "ARCH_REVIEW") {
        setArchitecture(await getArchitecture(run.threadId));
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  async function approveArchitecture() {
    if (!run) return;
    setBusy(true);
    try {
      const res = await resumeRun(run.threadId, "APPROVED");
      setPhase(res.phase);
      if (res.phase === "DELIVERY") {
        setTransformation(await getTransformation(run.threadId));
        setValidation(await getValidation(run.threadId));
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  const bsgApproved = phase !== "BSG_REVIEW" && run != null;
  const atArchGate = phase === "ARCH_REVIEW";
  const built = phase === "DELIVERY";

  return (
    <section className="page-stack">
      <section className="surface page-hero">
        <div>
          <p className="eyebrow">Migration run</p>
          <h1>Review the Behavioral Specification Graph before any code is transformed.</h1>
          <p className="surface-note">
            Discovery and the Analysis Agent build a BSG from your codebase. You approve it in plain
            English — the trust boundary between AI understanding and AI output.
          </p>
        </div>
        <div className="hero-badges">
          <span>Human-in-the-loop</span>
          <span>Durable gate</span>
          <span>Auditable</span>
        </div>
      </section>

      {error && <div className="error-banner">{error}</div>}

      {!run && (
        <section className="surface upload-panel">
          <div className="surface-head">
            <div>
              <p className="eyebrow">Start</p>
              <h2>Run discovery + analysis</h2>
            </div>
            <p className="surface-note">Upload a Java source zip, or try the bundled sample.</p>
          </div>

          <div className="field">
            <label htmlFor="run-name">Project name</label>
            <input id="run-name" type="text" placeholder="Optional label" value={name}
              onChange={(e) => setName(e.target.value)} disabled={busy} />
          </div>
          <div className="field">
            <label htmlFor="run-zip">Source zip</label>
            <input id="run-zip" type="file" accept=".zip"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)} disabled={busy} />
            <p className="field-help">{file ? file.name : "No file selected yet."}</p>
          </div>

          <div className="action-row">
            <button className="button button-primary" disabled={busy || !file}
              onClick={() => file && begin(() => startRunUpload(file, name))}>
              {busy ? "Running…" : "Start migration run"}
            </button>
            <button className="button button-secondary" disabled={busy}
              onClick={() => begin(() => startRunPath(SAMPLE_PATH, "sample"))}>
              Try sample project
            </button>
          </div>
        </section>
      )}

      {run && bsg && (
        <section className="surface">
          <div className="surface-head">
            <div>
              <p className="eyebrow">Gate #1 · BSG review</p>
              <h2>BSG review — {bsg.nodes.length} nodes</h2>
            </div>
            <p className="surface-note">
              {bsgApproved
                ? "Approved. The run has advanced past the trust boundary."
                : "Approve, edit or reject each rule, then approve the BSG to continue."}
            </p>
          </div>

          <BsgReviewBoard bsg={bsg} onReview={onReview} busy={busy || bsgApproved} />

          <div className="action-row bsg-approve-row">
            <button className="button button-primary" disabled={busy || bsgApproved} onClick={approveBsg}>
              {bsgApproved ? "BSG approved ✓" : "Approve BSG & continue"}
            </button>
            {bsgApproved && <span className="signal-pill signal-pill-ok">Gate #1 passed</span>}
          </div>
        </section>
      )}

      {run && architecture && (bsgApproved) && (
        <section className="surface">
          <div className="surface-head">
            <div>
              <p className="eyebrow">Gate #2 · Architecture review</p>
              <h2>Proposed target architecture</h2>
            </div>
            <p className="surface-note">
              {built
                ? "Architecture approved. The run is ready to build."
                : "The Architecture Agent mapped the approved BSG onto a layered target design."}
            </p>
          </div>

          <ArchitectureView plan={architecture} />

          <div className="action-row bsg-approve-row">
            <button
              className="button button-primary"
              disabled={busy || built || !atArchGate}
              onClick={approveArchitecture}
            >
              {built ? "Architecture approved ✓" : "Approve architecture & continue"}
            </button>
            {built && <span className="signal-pill signal-pill-ok">Gate #2 passed → build</span>}
          </div>
        </section>
      )}

      {run && transformation && (
        <section className="surface">
          <div className="surface-head">
            <div>
              <p className="eyebrow">Build · Transformation + Test Generation</p>
              <h2>Generated code &amp; tests</h2>
            </div>
            <p className="surface-note">
              Target code compile-checked in the sandbox; every module traces its BSG rules and
              every business rule gets a JUnit 5 test.
            </p>
          </div>
          {validation && (
            <div className="arch-meta">
              <span className={`bsg-pill ${validation.passed ? "bsg-pill-approved" : "bsg-pill-rejected"}`}>
                Validation {validation.passed ? "PASSED" : "FAILED"}
              </span>
              <span className="section-meta">compile {validation.compileOk ? "ok" : "failed"}</span>
              <span className="section-meta">
                BSG coverage {validation.coveragePercent}% ({validation.coveredNodeCount}/{validation.bsgNodeCount})
              </span>
            </div>
          )}
          <TransformationView result={transformation} />
        </section>
      )}
    </section>
  );
}
