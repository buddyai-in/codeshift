import { useEffect, useState } from "react";
import {
  checkCompliance,
  getComplianceTemplate,
  getStandards,
  type ComplianceReport,
  type ComplianceStandardId,
  type StandardSummary,
} from "../api";

const GENERIC_BSG = {
  projectId: "sample",
  versionNumber: 1,
  edges: [],
  nodes: [
    {
      nodeRef: "BSG-001",
      nodeType: "BUSINESS_RULE" as const,
      title: "Order total",
      description: "Sums the order line items.",
      sourceLocation: "OrderService",
      confidence: "HIGH" as const,
      humanStatus: "APPROVED" as const,
      origin: "MIGRATED",
      targetCodeLocation: null,
      testCoverage: true,
    },
  ],
};

export default function CompliancePage() {
  const [standards, setStandards] = useState<StandardSummary[]>([]);
  const [standard, setStandard] = useState<ComplianceStandardId>("PCI_DSS");
  const [report, setReport] = useState<ComplianceReport | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    getStandards().then(setStandards).catch((e) => setError(String(e)));
  }, []);

  async function guard(fn: () => Promise<ComplianceReport>) {
    setError("");
    setBusy(true);
    try {
      setReport(await fn());
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  async function checkTemplate() {
    await guard(async () => checkCompliance(standard, await getComplianceTemplate(standard)));
  }

  async function checkGeneric() {
    await guard(() => checkCompliance(standard, GENERIC_BSG));
  }

  return (
    <section className="page-stack">
      <section className="surface page-hero">
        <div>
          <p className="eyebrow">Compliance packs</p>
          <h1>PCI-DSS &amp; HIPAA control coverage, straight from the BSG.</h1>
          <p className="surface-note">
            Each vertical ships a control pack. We check the behavioral spec against every
            required control and flag the gaps with remediation — the report pack a regulated
            migration needs on day one.
          </p>
        </div>
        <div className="hero-badges">
          <span>PCI-DSS v4.0</span>
          <span>HIPAA</span>
          <span>Gap remediation</span>
        </div>
      </section>

      {error && <div className="error-banner">{error}</div>}

      <section className="surface">
        <div className="surface-head">
          <div>
            <p className="eyebrow">Standard</p>
            <h2>Choose a pack</h2>
          </div>
        </div>
        <div className="field">
          <label htmlFor="std">Compliance standard</label>
          <select id="std" className="bsg-title-input" value={standard} disabled={busy}
            onChange={(e) => setStandard(e.target.value as ComplianceStandardId)}>
            {standards.map((s) => (
              <option key={s.standard} value={s.standard}>
                {s.reference} — {s.controlCount} controls
              </option>
            ))}
          </select>
        </div>
        <div className="action-row">
          <button className="button button-primary" disabled={busy} onClick={checkTemplate}>
            Check seeded template (100%)
          </button>
          <button className="button button-secondary" disabled={busy} onClick={checkGeneric}>
            Check a generic migrated BSG (gaps)
          </button>
        </div>
      </section>

      {report && (
        <section className="surface">
          <div className="surface-head">
            <div>
              <p className="eyebrow">{report.reference}</p>
              <h2>Coverage report</h2>
            </div>
            <span className={`bsg-pill ${report.passed ? "bsg-pill-approved" : "bsg-pill-rejected"}`}>
              {report.score}/100 · {report.coveredControls}/{report.totalControls} controls
            </span>
          </div>
          <div className="arch-table">
            {report.results.map((r) => (
              <div className="arch-row" key={r.controlId}>
                <code className="arch-module">{r.controlId}</code>
                <span className="section-meta arch-target">{r.title}</span>
                {r.covered ? (
                  <span className="signal-pill signal-pill-ok">
                    covered · {r.matchedNodeRefs.join(", ")}
                  </span>
                ) : (
                  <span className="bsg-pill bsg-pill-rejected">gap — {r.remediation}</span>
                )}
              </div>
            ))}
          </div>
        </section>
      )}
    </section>
  );
}
