import { useState } from "react";
import type { TransformationResult } from "../api";

export default function TransformationView({ result }: { result: TransformationResult }) {
  const [openModule, setOpenModule] = useState<string | null>(
    result.modules[0]?.moduleId ?? null,
  );
  const compiledCount = result.modules.filter((m) => m.compiled).length;

  return (
    <div className="xform-view">
      <div className="arch-meta">
        <span className={`bsg-pill ${result.allCompiled ? "bsg-pill-approved" : "bsg-pill-rejected"}`}>
          {result.allCompiled ? "All compiled ✓" : "Compile issues"}
        </span>
        <span className="section-meta">
          {compiledCount}/{result.modules.length} modules compiled
        </span>
        <span className="section-meta">{result.tests.length} tests generated</span>
      </div>

      <section className="section-block">
        <div className="section-title-row">
          <h3>Generated modules</h3>
        </div>
        <div className="xform-modules">
          {result.modules.map((m) => (
            <div className="xform-module" key={m.moduleId}>
              <button
                className="xform-module-head"
                onClick={() => setOpenModule(openModule === m.moduleId ? null : m.moduleId)}
              >
                <code>{m.targetClass}</code>
                <span className={`arch-layer arch-layer-${m.layer.toLowerCase()}`}>{m.layer}</span>
                <span className={`bsg-status bsg-status-${m.compiled ? "approved" : "rejected"}`}>
                  {m.compiled ? "compiled" : "failed"}
                </span>
                {m.bsgRuleRefs.length > 0 && (
                  <span className="section-meta">{m.bsgRuleRefs.join(", ")}</span>
                )}
              </button>
              {openModule === m.moduleId && <pre className="xform-code">{m.sourceCode}</pre>}
            </div>
          ))}
        </div>
      </section>

      {result.tests.length > 0 && (
        <section className="section-block">
          <div className="section-title-row">
            <h3>Generated tests</h3>
            <span className="section-meta">JUnit 5 · BSG-traceable</span>
          </div>
          <div className="xform-tests">
            {result.tests.map((t) => (
              <div className="xform-test" key={t.testClass}>
                <code>{t.testClass}</code>
                <span className="bsg-pill bsg-pill-modified">{t.bsgRuleRef}</span>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
