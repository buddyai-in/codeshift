import { useEffect, useState } from "react";
import {
  addFeature,
  createProject,
  getBsgVersions,
  listProjects,
  seedBsg,
  type BsgGraph,
  type ProjectSummary,
  type VersionSummary,
} from "../api";

const SAMPLE_BSG = (projectId: string): BsgGraph => ({
  projectId,
  versionNumber: 1,
  nodes: [
    {
      nodeRef: "BSG-001",
      nodeType: "BUSINESS_RULE",
      title: "Order total",
      description: "Sums the order line items.",
      sourceLocation: "OrderService",
      confidence: "HIGH",
      humanStatus: "APPROVED",
      origin: "MIGRATED",
      targetCodeLocation: null,
      testCoverage: true,
    },
  ],
  edges: [],
});

export default function NewCodePage() {
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [selected, setSelected] = useState<string>("");
  const [versions, setVersions] = useState<VersionSummary[]>([]);
  const [name, setName] = useState("");
  const [feature, setFeature] = useState("");
  const [resultBsg, setResultBsg] = useState<BsgGraph | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function guard<T>(fn: () => Promise<T>): Promise<T | undefined> {
    setError("");
    setBusy(true);
    try {
      return await fn();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
      return undefined;
    } finally {
      setBusy(false);
    }
  }

  async function refreshProjects() {
    const p = await guard(() => listProjects());
    if (p) setProjects(p);
  }

  useEffect(() => {
    refreshProjects();
  }, []);

  async function loadVersions(projectId: string) {
    setSelected(projectId);
    setResultBsg(null);
    const v = await guard(() => getBsgVersions(projectId));
    if (v) setVersions(v);
  }

  async function onCreate() {
    const c = await guard(() => createProject(name || "untitled", "JAVA_8", "JAVA_21_SPRING_BOOT"));
    if (c) {
      setName("");
      await refreshProjects();
      await loadVersions(c.projectId);
    }
  }

  async function onSeed() {
    if (!selected) return;
    await guard(() => seedBsg(selected, SAMPLE_BSG(selected)));
    await loadVersions(selected);
  }

  async function onAddFeature() {
    if (!selected || !feature.trim()) return;
    const r = await guard(() => addFeature(selected, feature.trim()));
    if (r) {
      setResultBsg(r.bsg);
      setFeature("");
      await loadVersions(selected);
    }
  }

  return (
    <section className="page-stack">
      <section className="surface page-hero">
        <div>
          <p className="eyebrow">New code addition</p>
          <h1>Add features to a migrated system — the BSG stays a living document.</h1>
          <p className="surface-note">
            Every feature request becomes new <code>NEW_FEATURE</code> BSG nodes in a new version.
            The whole application history is a traceable chain of human-approved requests.
          </p>
        </div>
        <div className="hero-badges">
          <span>Versioned BSG</span>
          <span>Requirements Agent</span>
          <span>Audit trail</span>
        </div>
      </section>

      {error && <div className="error-banner">{error}</div>}

      <div className="dashboard-grid">
        <aside className="dashboard-side">
          <section className="surface">
            <div className="surface-head">
              <div>
                <p className="eyebrow">Projects</p>
                <h2>Create or select</h2>
              </div>
            </div>
            <div className="field">
              <label htmlFor="proj-name">New project name</label>
              <input id="proj-name" type="text" value={name} disabled={busy}
                onChange={(e) => setName(e.target.value)} placeholder="acme-orders" />
            </div>
            <div className="action-row">
              <button className="button button-primary" disabled={busy} onClick={onCreate}>
                Create project
              </button>
            </div>
            <div className="rail-menu" style={{ marginTop: 14 }}>
              {projects.map((p) => (
                <button
                  key={p.id}
                  className={`rail-menu-item ${selected === p.id ? "rail-menu-item-active" : ""}`}
                  onClick={() => loadVersions(p.id)}
                >
                  {p.name}
                </button>
              ))}
              {projects.length === 0 && <p className="surface-note">No projects yet.</p>}
            </div>
          </section>
        </aside>

        <section className="dashboard-main">
          {!selected ? (
            <section className="surface empty-state">
              <p className="eyebrow">No project selected</p>
              <h2>Create or pick a project to add features.</h2>
              <p className="surface-note">
                These endpoints require the database profile (not <code>nodb</code>).
              </p>
            </section>
          ) : (
            <div className="content-stack">
              <section className="surface">
                <div className="surface-head">
                  <div>
                    <p className="eyebrow">BSG versions</p>
                    <h2>Audit trail</h2>
                  </div>
                  {versions.length === 0 && (
                    <button className="button button-secondary" disabled={busy} onClick={onSeed}>
                      Seed initial BSG
                    </button>
                  )}
                </div>
                <div className="bsg-summary">
                  {versions.map((v) => (
                    <span key={v.versionId} className="bsg-pill bsg-pill-approved">
                      v{v.versionNumber} · {v.nodeCount} nodes{v.approved ? " · approved" : ""}
                    </span>
                  ))}
                  {versions.length === 0 && <p className="surface-note">No BSG yet — seed one to start.</p>}
                </div>
              </section>

              <section className="surface">
                <div className="surface-head">
                  <div>
                    <p className="eyebrow">Requirements Agent</p>
                    <h2>Add a feature</h2>
                  </div>
                </div>
                <div className="field">
                  <label htmlFor="feature">Feature request (plain English)</label>
                  <input id="feature" type="text" value={feature} disabled={busy || versions.length === 0}
                    onChange={(e) => setFeature(e.target.value)}
                    placeholder="When an order ships, send an SMS via Twilio with a tracking link" />
                </div>
                <div className="action-row">
                  <button className="button button-primary" disabled={busy || versions.length === 0 || !feature.trim()}
                    onClick={onAddFeature}>
                    Add feature → new BSG version
                  </button>
                </div>

                {resultBsg && (
                  <div className="bsg-board" style={{ marginTop: 16 }}>
                    {resultBsg.nodes.map((n) => (
                      <article key={n.nodeRef}
                        className={`bsg-card ${n.origin === "NEW_FEATURE" ? "bsg-card-modified" : ""}`}>
                        <div className="bsg-card-head">
                          <span className="bsg-ref">{n.nodeRef}</span>
                          <span className="bsg-type">{n.nodeType.replaceAll("_", " ")}</span>
                          {n.origin === "NEW_FEATURE" && (
                            <span className="bsg-status bsg-status-modified">NEW FEATURE</span>
                          )}
                        </div>
                        <strong>{n.title}</strong>
                        <p className="surface-note" style={{ margin: 0 }}>{n.description}</p>
                      </article>
                    ))}
                  </div>
                )}
              </section>
            </div>
          )}
        </section>
      </div>
    </section>
  );
}
