import type { AssessmentReport } from "../api";

function Stat({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="stat">
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
      {hint && <div className="stat-hint">{hint}</div>}
    </div>
  );
}

const usd = (n: number) => "$" + n.toLocaleString("en-US");

export default function ReportView({ report }: { report: AssessmentReport }) {
  return (
    <section className="panel report">
      <div className="report-head">
        <h2>{report.projectName}</h2>
        <span className={`tier tier-${report.suggestedTier.toLowerCase()}`}>
          {report.suggestedTier} tier
        </span>
      </div>

      <div className="stats">
        <Stat label="Modules" value={String(report.moduleCount)} />
        <Stat label="Est. LOC" value={report.estimatedLoc.toLocaleString("en-US")} />
        <Stat label="Dependencies" value={String(report.dependencyEdgeCount)} />
        <Stat label="Entry points" value={String(report.entryPointCount)} />
        <Stat label="Complexity" value={`${report.complexityScore}/100`} />
        <Stat label="Est. effort" value={`${report.estimatedEffortDays}d`} />
        <Stat label="Price estimate" value={usd(report.priceEstimateUsd)} hint="≈ $50 / kLOC" />
        <Stat label="Cycles" value={report.hasCycles ? "Yes" : "None"} />
      </div>

      {report.messagingSystems.length > 0 && (
        <div className="chips">
          <span className="chips-label">Messaging</span>
          {report.messagingSystems.map((m) => (
            <span key={m} className="chip chip-warn">
              {m}
            </span>
          ))}
        </div>
      )}

      <h3>Migration signals</h3>
      <ul className="signals">
        {report.migrationSignals.map((s, i) => (
          <li key={i}>{s}</li>
        ))}
      </ul>

      <details className="order">
        <summary>Translation order (leaf-first) — {report.translationOrder.length} modules</summary>
        <ol>
          {report.translationOrder.map((m) => (
            <li key={m}>
              <code>{m}</code>
            </li>
          ))}
        </ol>
      </details>
    </section>
  );
}
