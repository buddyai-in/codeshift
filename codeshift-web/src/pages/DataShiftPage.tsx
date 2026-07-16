import { useState } from "react";
import { convertDdl, type DataShiftResult } from "../api";

const SAMPLE_DDL = `CREATE TABLE customer (
  id        NUMBER(10),
  balance   NUMBER(12,2),
  name      VARCHAR2(100),
  notes     CLOB,
  avatar    BLOB,
  created   DATE DEFAULT SYSDATE
);

SELECT NVL(name, 'n/a') FROM customer WHERE ROWNUM <= 10;`;

export default function DataShiftPage() {
  const [ddl, setDdl] = useState(SAMPLE_DDL);
  const [result, setResult] = useState<DataShiftResult | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function onConvert() {
    setError("");
    setBusy(true);
    try {
      setResult(await convertDdl(ddl));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="page-stack">
      <section className="surface page-hero">
        <div>
          <p className="eyebrow">DataShift</p>
          <h1>Oracle → PostgreSQL DDL, converted deterministically.</h1>
          <p className="surface-note">
            Type mappings (NUMBER, VARCHAR2, CLOB, BLOB, DATE, RAW…) and function rewrites
            (SYSDATE, NVL, SYS_GUID…) applied in-process — no live database required. Every
            substitution is audited; anything that needs a human is flagged, not silently dropped.
          </p>
        </div>
        <div className="hero-badges">
          <span>Deterministic</span>
          <span>Audited</span>
          <span>No DB needed</span>
        </div>
      </section>

      {error && <div className="error-banner">{error}</div>}

      <div className="dashboard-grid">
        <section className="dashboard-main">
          <section className="surface">
            <div className="surface-head">
              <div>
                <p className="eyebrow">Source</p>
                <h2>Oracle DDL / SQL</h2>
              </div>
              <button className="button button-primary" disabled={busy || !ddl.trim()} onClick={onConvert}>
                Convert →
              </button>
            </div>
            <textarea
              className="ddl-input"
              value={ddl}
              disabled={busy}
              rows={14}
              spellCheck={false}
              onChange={(e) => setDdl(e.target.value)}
            />
          </section>

          {result && (
            <section className="surface">
              <div className="surface-head">
                <div>
                  <p className="eyebrow">{result.targetDialect}</p>
                  <h2>Converted DDL</h2>
                </div>
              </div>
              <pre className="ddl-output">{result.convertedDdl}</pre>
            </section>
          )}
        </section>

        <aside className="dashboard-side">
          {result && (
            <>
              <section className="surface">
                <div className="surface-head">
                  <div>
                    <p className="eyebrow">Applied</p>
                    <h2>Mappings</h2>
                  </div>
                </div>
                <div className="arch-table">
                  {result.mappings.map((m, i) => (
                    <div className="arch-row" key={i}>
                      <code className="arch-module">{m.source}</code>
                      <span className="section-meta arch-target">→ {m.target}</span>
                      <span className="bsg-pill bsg-pill-approved">×{m.occurrences}</span>
                    </div>
                  ))}
                  {result.mappings.length === 0 && (
                    <p className="surface-note">No substitutions applied.</p>
                  )}
                </div>
              </section>

              {result.warnings.length > 0 && (
                <section className="surface">
                  <div className="surface-head">
                    <div>
                      <p className="eyebrow">Needs review</p>
                      <h2>Warnings</h2>
                    </div>
                  </div>
                  <div className="bsg-summary">
                    {result.warnings.map((w, i) => (
                      <p key={i} className="surface-note" style={{ margin: 0 }}>
                        ⚠ {w}
                      </p>
                    ))}
                  </div>
                </section>
              )}
            </>
          )}
        </aside>
      </div>
    </section>
  );
}
