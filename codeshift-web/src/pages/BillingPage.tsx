import { useEffect, useState } from "react";
import {
  getBudget,
  getInvoice,
  getUsage,
  listProjects,
  recordUsage,
  setBudget,
  type BudgetResponse,
  type Invoice,
  type ProjectSummary,
  type UsageRecord,
} from "../api";

const MODELS = [
  "claude-opus-4-8",
  "claude-sonnet-4-6",
  "claude-haiku-4-5-20251001",
  "gpt-4.1",
];

export default function BillingPage() {
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [selected, setSelected] = useState("");
  const [budget, setBudgetState] = useState<BudgetResponse | null>(null);
  const [usage, setUsage] = useState<UsageRecord[]>([]);
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [budgetInput, setBudgetInput] = useState("25");
  const [model, setModel] = useState(MODELS[2]);
  const [inTok, setInTok] = useState("50000");
  const [outTok, setOutTok] = useState("10000");
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

  useEffect(() => {
    guard(() => listProjects()).then((p) => p && setProjects(p));
  }, []);

  async function select(projectId: string) {
    setSelected(projectId);
    setInvoice(null);
    const b = await guard(() => getBudget(projectId));
    if (b) {
      setBudgetState(b);
      setBudgetInput(String(b.budgetUsd));
    }
    const u = await guard(() => getUsage());
    if (u) setUsage(u.filter((e) => e.projectId === projectId));
  }

  async function saveBudget() {
    if (!selected) return;
    const b = await guard(() => setBudget(selected, Number(budgetInput)));
    if (b) setBudgetState(b);
  }

  async function meter() {
    if (!selected) return;
    const res = await guard(() => recordUsage(selected, model, Number(inTok), Number(outTok)));
    if (res) {
      setBudgetState({
        budgetUsd: res.budgetUsd,
        spentUsd: res.spentUsd,
        remainingUsd: res.remainingUsd,
      });
      const u = await getUsage().catch(() => []);
      setUsage(u.filter((e) => e.projectId === selected));
    }
  }

  async function loadInvoice() {
    const inv = await guard(() => getInvoice());
    if (inv) setInvoice(inv);
  }

  return (
    <section className="page-stack">
      <section className="surface page-hero">
        <div>
          <p className="eyebrow">Billing &amp; metering</p>
          <h1>Every token metered against a budget — no surprise invoices.</h1>
          <p className="surface-note">
            Usage is priced through the model gateway's cost table and charged to a project
            budget. A call that would blow the budget is refused (402), and invoices roll usage
            up per project with a payment intent. These endpoints need the database profile.
          </p>
        </div>
        <div className="hero-badges">
          <span>Budgets in code</span>
          <span>Usage metering</span>
          <span>Invoices</span>
        </div>
      </section>

      {error && <div className="error-banner">{error}</div>}

      <div className="dashboard-grid">
        <aside className="dashboard-side">
          <section className="surface">
            <div className="surface-head">
              <div>
                <p className="eyebrow">Projects</p>
                <h2>Select</h2>
              </div>
            </div>
            <div className="rail-menu">
              {projects.map((p) => (
                <button key={p.id}
                  className={`rail-menu-item ${selected === p.id ? "rail-menu-item-active" : ""}`}
                  onClick={() => select(p.id)}>
                  {p.name}
                </button>
              ))}
              {projects.length === 0 && (
                <p className="surface-note">No projects (needs the DB profile).</p>
              )}
            </div>
          </section>
        </aside>

        <section className="dashboard-main">
          {!selected ? (
            <section className="surface empty-state">
              <p className="eyebrow">No project selected</p>
              <h2>Pick a project to meter.</h2>
            </section>
          ) : (
            <div className="content-stack">
              <section className="surface">
                <div className="surface-head">
                  <div>
                    <p className="eyebrow">Budget</p>
                    <h2>Guardrail</h2>
                  </div>
                  {budget && (
                    <span className={`bsg-pill ${budget.remainingUsd > 0 ? "bsg-pill-approved" : "bsg-pill-rejected"}`}>
                      ${budget.spentUsd.toFixed(4)} / ${budget.budgetUsd.toFixed(2)} spent
                    </span>
                  )}
                </div>
                <div className="field">
                  <label htmlFor="budget">Budget (USD)</label>
                  <input id="budget" type="number" value={budgetInput} disabled={busy}
                    onChange={(e) => setBudgetInput(e.target.value)} />
                </div>
                <div className="action-row">
                  <button className="button button-secondary" disabled={busy} onClick={saveBudget}>
                    Save budget
                  </button>
                </div>
              </section>

              <section className="surface">
                <div className="surface-head">
                  <div>
                    <p className="eyebrow">Meter a call</p>
                    <h2>Record usage</h2>
                  </div>
                </div>
                <div className="action-row" style={{ flexWrap: "wrap", gap: 10 }}>
                  <select className="bsg-title-input" value={model} disabled={busy}
                    onChange={(e) => setModel(e.target.value)}>
                    {MODELS.map((m) => <option key={m} value={m}>{m}</option>)}
                  </select>
                  <input type="number" value={inTok} disabled={busy} style={{ maxWidth: 130 }}
                    onChange={(e) => setInTok(e.target.value)} placeholder="input tokens" />
                  <input type="number" value={outTok} disabled={busy} style={{ maxWidth: 130 }}
                    onChange={(e) => setOutTok(e.target.value)} placeholder="output tokens" />
                  <button className="button button-primary" disabled={busy} onClick={meter}>
                    Record
                  </button>
                </div>
                <div className="arch-table" style={{ marginTop: 12 }}>
                  {usage.map((u) => (
                    <div className="arch-row" key={u.id}>
                      <code className="arch-module">{u.model}</code>
                      <span className="section-meta arch-target">
                        {u.inputTokens.toLocaleString()} in · {u.outputTokens.toLocaleString()} out
                      </span>
                      <span className="bsg-pill bsg-pill-approved">${u.costUsd.toFixed(4)}</span>
                    </div>
                  ))}
                  {usage.length === 0 && <p className="surface-note">No usage recorded yet.</p>}
                </div>
              </section>

              <section className="surface">
                <div className="surface-head">
                  <div>
                    <p className="eyebrow">Invoice</p>
                    <h2>Tenant roll-up</h2>
                  </div>
                  <button className="button button-secondary" disabled={busy} onClick={loadInvoice}>
                    Generate invoice
                  </button>
                </div>
                {invoice && (
                  <>
                    <div className="arch-table">
                      {invoice.lineItems.map((li) => (
                        <div className="arch-row" key={li.projectId}>
                          <code className="arch-module">{li.projectName}</code>
                          <span className="section-meta arch-target">{li.calls} calls</span>
                          <span className="bsg-pill bsg-pill-approved">${li.amountUsd.toFixed(4)}</span>
                        </div>
                      ))}
                    </div>
                    <div className="arch-meta" style={{ marginTop: 12 }}>
                      <span className="bsg-pill bsg-pill-approved">
                        Total ${invoice.totalUsd.toFixed(4)} {invoice.currency}
                      </span>
                      <span className="section-meta">
                        {invoice.payment.provider} · {invoice.payment.status}
                      </span>
                    </div>
                  </>
                )}
              </section>
            </div>
          )}
        </section>
      </div>
    </section>
  );
}
