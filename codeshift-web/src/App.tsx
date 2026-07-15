import { useState } from "react";
import UploadPanel from "./components/UploadPanel";
import ReportView from "./components/ReportView";
import DependencyGraph from "./components/DependencyGraph";
import type { AssessmentResult } from "./api";

export default function App() {
  const [result, setResult] = useState<AssessmentResult | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          Code<span>Shift</span>
        </div>
        <div className="tagline">Free migration assessment</div>
      </header>

      <main>
        <UploadPanel
          onResult={setResult}
          onError={setError}
          onLoading={setLoading}
          loading={loading}
        />

        {error && <div className="error">{error}</div>}

        {result && (
          <div className="results">
            <ReportView report={result.report} />
            <section className="panel">
              <h2>Dependency graph</h2>
              <p className="muted">
                Leaf-first, left → right. Highlighted nodes are entry points (controllers / mains).
              </p>
              <DependencyGraph graph={result.graph} />
            </section>
          </div>
        )}

        {!result && !loading && !error && (
          <p className="hint">Upload a codebase or try the sample to see a live assessment.</p>
        )}
      </main>

      <footer className="foot">
        CodeShift · Phase 1 demo · deterministic analysis, no LLM required
      </footer>
    </div>
  );
}
