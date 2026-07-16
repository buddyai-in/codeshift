import { useEffect, useState } from "react";
import { getPortfolio, type PortfolioReport } from "../api";

const gradeOk = (g: string) => g === "A" || g === "B";

export default function PortfolioPage() {
  const [portfolio, setPortfolio] = useState<PortfolioReport | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(true);

  useEffect(() => {
    getPortfolio()
      .then(setPortfolio)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)))
      .finally(() => setBusy(false));
  }, []);

  return (
    <section className="page-stack">
      <section className="surface page-hero">
        <div>
          <p className="eyebrow">Portfolio intelligence</p>
          <h1>CIO-level health across every application — one BSG pipeline, rolled up.</h1>
          <p className="surface-note">
            Every project's version count, BSG size and debt grade, derived from the same
            analysis pipeline. The view no CIO has had before.
          </p>
        </div>
        <div className="hero-badges">
          <span>Cross-app</span>
          <span>Debt-ranked</span>
          <span>Live</span>
        </div>
      </section>

      {error && <div className="error-banner">{error}</div>}

      {portfolio && (
        <>
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-value">{portfolio.projectCount}</div>
              <div className="stat-label">Applications</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{portfolio.avgDebtScore}/100</div>
              <div className="stat-label">Avg debt score</div>
            </div>
          </div>

          <section className="surface">
            <div className="surface-head">
              <div>
                <p className="eyebrow">Applications</p>
                <h2>Health dashboard</h2>
              </div>
            </div>
            <div className="arch-table">
              {portfolio.projects.map((p) => (
                <div className="arch-row" key={p.projectId}>
                  <code className="arch-module">{p.name}</code>
                  <span className={`bsg-pill ${gradeOk(p.debtGrade) ? "bsg-pill-approved" : "bsg-pill-rejected"}`}>
                    Debt {p.debtGrade} · {p.debtScore}/100
                  </span>
                  <span className="section-meta arch-target">
                    v{p.versionCount} · {p.nodeCount} rules
                  </span>
                </div>
              ))}
              {portfolio.projects.length === 0 && (
                <p className="surface-note">No projects yet. Create one on the New code page.</p>
              )}
            </div>
          </section>
        </>
      )}

      {busy && !portfolio && !error && <p className="hint">Loading portfolio…</p>}
    </section>
  );
}
