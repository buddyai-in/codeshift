import { useState } from "react";
import { Link } from "react-router-dom";
import { createOrg, createProject, getActiveTenant, setActiveTenant } from "../api";

type Step = 1 | 2 | 3;

export default function OnboardingPage() {
  const [step, setStep] = useState<Step>(1);
  const [orgName, setOrgName] = useState("");
  const [orgId, setOrgId] = useState<string | null>(getActiveTenant());
  const [projectName, setProjectName] = useState("");
  const [projectId, setProjectId] = useState<string | null>(null);
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

  async function onCreateOrg() {
    const res = await guard(() => createOrg(orgName || "my-org"));
    if (res) {
      setActiveTenant(res.orgId); // scope every later request to this tenant
      setOrgId(res.orgId);
      setStep(2);
    }
  }

  async function onCreateProject() {
    const res = await guard(() =>
      createProject(projectName || "first-project", "JAVA_8", "JAVA_21_SPRING_BOOT"),
    );
    if (res) {
      setProjectId(res.projectId);
      setStep(3);
    }
  }

  const dot = (n: Step) => (step >= n ? "status-dot-ready" : "status-dot-loading");

  return (
    <section className="page-stack">
      <section className="surface page-hero">
        <div>
          <p className="eyebrow">Onboarding</p>
          <h1>Set up your tenant in three steps.</h1>
          <p className="surface-note">
            Create your organization, spin up a first project, then head into a migration or
            new-code run. Everything you do is scoped to this tenant via the X-Tenant-Id header.
          </p>
        </div>
        <div className="hero-badges">
          <span className="status-chip"><span className={`status-dot ${dot(1)}`} />Org</span>
          <span className="status-chip"><span className={`status-dot ${dot(2)}`} />Project</span>
          <span className="status-chip"><span className={`status-dot ${dot(3)}`} />Go</span>
        </div>
      </section>

      {error && <div className="error-banner">{error}</div>}
      {getActiveTenant() && (
        <p className="surface-note">
          Active tenant: <code>{getActiveTenant()}</code>
        </p>
      )}

      {step === 1 && (
        <section className="surface">
          <div className="surface-head">
            <div>
              <p className="eyebrow">Step 1</p>
              <h2>Create your organization</h2>
            </div>
          </div>
          <div className="field">
            <label htmlFor="org">Organization name</label>
            <input id="org" type="text" value={orgName} disabled={busy}
              onChange={(e) => setOrgName(e.target.value)} placeholder="Acme Corp" />
          </div>
          <div className="action-row">
            <button className="button button-primary" disabled={busy} onClick={onCreateOrg}>
              Create org &amp; continue
            </button>
          </div>
          <p className="surface-note">Needs the database profile (not <code>nodb</code>).</p>
        </section>
      )}

      {step === 2 && (
        <section className="surface">
          <div className="surface-head">
            <div>
              <p className="eyebrow">Step 2</p>
              <h2>Create your first project</h2>
            </div>
          </div>
          <div className="field">
            <label htmlFor="proj">Project name</label>
            <input id="proj" type="text" value={projectName} disabled={busy}
              onChange={(e) => setProjectName(e.target.value)} placeholder="acme-orders" />
          </div>
          <div className="action-row">
            <button className="button button-primary" disabled={busy} onClick={onCreateProject}>
              Create project &amp; continue
            </button>
          </div>
        </section>
      )}

      {step === 3 && (
        <section className="surface">
          <div className="surface-head">
            <div>
              <p className="eyebrow">Step 3 · Ready</p>
              <h2>Your tenant is set up</h2>
            </div>
            <span className="signal-pill signal-pill-ok">Done</span>
          </div>
          <p className="surface-note">
            Org <code>{orgId}</code> and project <code>{projectId}</code> are live. Next:
          </p>
          <div className="action-row" style={{ flexWrap: "wrap", gap: 10 }}>
            <Link className="button button-primary" to="/migrate">Start a migration run</Link>
            <Link className="button button-secondary" to="/new-code">Add new code</Link>
            <Link className="button button-secondary" to="/billing">Set a budget</Link>
            <Link className="button button-secondary" to="/compliance">Compliance packs</Link>
          </div>
        </section>
      )}
    </section>
  );
}
