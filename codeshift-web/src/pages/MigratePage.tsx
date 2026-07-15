import { useState } from "react";
import BsgReviewBoard from "../components/BsgReviewBoard";
import {
  getBsg,
  resumeRun,
  reviewNode,
  startRunPath,
  startRunUpload,
  type BsgGraph,
  type HumanStatus,
  type RunStart,
} from "../api";

const SAMPLE_PATH = "codeshift-parser/src/test/resources/sample-project";

export default function MigratePage() {
  const [file, setFile] = useState<File | null>(null);
  const [name, setName] = useState("");
  const [run, setRun] = useState<RunStart | null>(null);
  const [bsg, setBsg] = useState<BsgGraph | null>(null);
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

  async function approveAll() {
    if (!run) return;
    setBusy(true);
    try {
      const res = await resumeRun(run.threadId, "APPROVED");
      setPhase(res.phase);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  const approved = phase !== "BSG_REVIEW" && run != null;

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
              <p className="eyebrow">Gate · {phase.replaceAll("_", " ")}</p>
              <h2>BSG review — {bsg.nodes.length} nodes</h2>
            </div>
            <p className="surface-note">
              {approved
                ? "Approved. The run has advanced past the gate."
                : "Approve, edit or reject each rule, then approve the BSG to continue."}
            </p>
          </div>

          <BsgReviewBoard bsg={bsg} onReview={onReview} busy={busy || approved} />

          <div className="action-row bsg-approve-row">
            <button className="button button-primary" disabled={busy || approved} onClick={approveAll}>
              {approved ? `Advanced to ${phase.replaceAll("_", " ")}` : "Approve BSG & continue"}
            </button>
            {approved && <span className="signal-pill signal-pill-ok">Gate passed → {phase}</span>}
          </div>
        </section>
      )}
    </section>
  );
}
