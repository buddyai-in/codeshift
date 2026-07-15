import DependencyGraph from "../components/DependencyGraph";
import ReportView from "../components/ReportView";
import UploadPanel from "../components/UploadPanel";
import type { AssessmentResult } from "../api";

interface DashboardPageProps {
  result: AssessmentResult | null;
  setResult: (result: AssessmentResult) => void;
  error: string;
  setError: (error: string) => void;
  loading: boolean;
  setLoading: (loading: boolean) => void;
}

export default function DashboardPage({
  result,
  setResult,
  error,
  setError,
  loading,
  setLoading,
}: DashboardPageProps) {
  return (
    <section className="page-stack">
      <section className="surface page-hero">
        <div>
          <p className="eyebrow">Dashboard</p>
          <h1>Analyze architecture, quantify migration effort, and map dependency order.</h1>
          <p className="surface-note">
            Upload a Java codebase and move from discovery to execution with scorecards and a leaf-first graph.
          </p>
        </div>
        <div className="hero-badges">
          <span>Deterministic</span>
          <span>Leaf-first</span>
          <span>Route-driven</span>
        </div>
      </section>

      {error && <div className="error-banner">{error}</div>}

      <div className="dashboard-grid">
        <aside className="dashboard-side">
          <UploadPanel onResult={setResult} onError={setError} onLoading={setLoading} loading={loading} />
        </aside>

        <section className="dashboard-main">
          {result ? (
            <div className="content-stack">
              <ReportView report={result.report} />
              <section className="surface graph-shell">
                <div className="surface-head">
                  <div>
                    <p className="eyebrow">Dependency graph</p>
                    <h2>Left-to-right module flow</h2>
                  </div>
                  <p className="surface-note">Entry points are shown on the right edge of the graph.</p>
                </div>
                <DependencyGraph graph={result.graph} />
              </section>
            </div>
          ) : (
            <section className="surface empty-state">
              <p className="eyebrow">No result yet</p>
              <h2>Run an assessment to populate this dashboard.</h2>
              <p className="surface-note">
                Once analysis completes, this page will show migration signals, effort estimates, and the dependency graph.
              </p>
              <div className="empty-steps">
                <article>
                  <span>01</span>
                  <strong>Upload zip</strong>
                  <p>Select your Java source archive.</p>
                </article>
                <article>
                  <span>02</span>
                  <strong>Analyze</strong>
                  <p>Wait for parser and scoring output.</p>
                </article>
                <article>
                  <span>03</span>
                  <strong>Plan</strong>
                  <p>Use the translation order in execution.</p>
                </article>
              </div>
            </section>
          )}
        </section>
      </div>
    </section>
  );
}
