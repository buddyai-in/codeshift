import type { AssessmentReport } from "../api";

function Stat({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="stat-card">
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
      {hint && <div className="stat-hint">{hint}</div>}
    </div>
  );
}

const usd = (n: number) => "$" + n.toLocaleString("en-US");

export default function ReportView({ report }: { report: AssessmentReport }) {
  const risk = report.hasCycles ? "Needs review" : "Healthy";
  const messaging = report.messagingSystems.length || 0;

  return (
    <section className="surface report-shell">
      <div className="surface-head report-head">
        <div>
          <p className="eyebrow">Assessment summary</p>
          <h2>{report.projectName}</h2>
          <p className="surface-note">
            {report.moduleCount} modules, {report.dependencyEdgeCount} dependency edges, and {report.entryPointCount} entry points.
          </p>
        </div>
        <div className="tier-stack">
          <span className={`tier tier-${report.suggestedTier.toLowerCase()}`}>{report.suggestedTier}</span>
          <span className={`signal-pill ${report.hasCycles ? "signal-pill-warn" : "signal-pill-ok"}`}>
            {risk}
          </span>
        </div>
      </div>

      <div className="stats-grid">
        <Stat label="Modules" value={String(report.moduleCount)} />
        <Stat label="Est. LOC" value={report.estimatedLoc.toLocaleString("en-US")} />
        <Stat label="Dependencies" value={String(report.dependencyEdgeCount)} />
        <Stat label="Entry points" value={String(report.entryPointCount)} />
        <Stat label="Complexity" value={`${report.complexityScore}/100`} />
        <Stat label="Est. effort" value={`${report.estimatedEffortDays}d`} />
        <Stat label="Price estimate" value={usd(report.priceEstimateUsd)} hint="≈ $50 / kLOC" />
        <Stat label="Cycles" value={report.hasCycles ? "Detected" : "None"} />
      </div>

      <div className="insight-grid">
        <div className="insight-card">
          <span className="insight-label">Messaging systems</span>
          <strong>{messaging}</strong>
          <p>{messaging > 0 ? report.messagingSystems.join(" • ") : "None detected"}</p>
        </div>
        <div className="insight-card">
          <span className="insight-label">javax usage</span>
          <strong>{report.usesJavaxNamespace ? "Present" : "None"}</strong>
          <p>{report.usesJavaxNamespace ? "Plan the namespace migration early." : "No namespace migration signal found."}</p>
        </div>
        <div className="insight-card">
          <span className="insight-label">Translation order</span>
          <strong>{report.translationOrder.length}</strong>
          <p>Modules in leaf-first order.</p>
        </div>
      </div>

      <section className="section-block">
        <div className="section-title-row">
          <h3>Migration signals</h3>
          <span className="section-meta">{report.migrationSignals.length} items</span>
        </div>
        <ul className="signal-list">
          {report.migrationSignals.map((s, i) => (
            <li key={i}>{s}</li>
          ))}
        </ul>
      </section>

      <section className="section-block">
        <div className="section-title-row">
          <h3>Leaf-first translation order</h3>
          <span className="section-meta">{report.translationOrder.length} modules</span>
        </div>
        <ol className="order-list">
          {report.translationOrder.map((m) => (
            <li key={m}>
              <code>{m}</code>
            </li>
          ))}
        </ol>
      </section>
    </section>
  );
}
