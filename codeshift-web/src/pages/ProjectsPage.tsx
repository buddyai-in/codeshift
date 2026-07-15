import type { AssessmentReport } from "../api";

interface ProjectsPageProps {
  report: AssessmentReport | null;
}

export default function ProjectsPage({ report }: ProjectsPageProps) {
  return (
    <section className="page-stack">
      <section className="surface">
        <p className="eyebrow">Projects</p>
        <h1>Portfolio view for migration programs.</h1>
        <p className="surface-note">
          Track where each codebase sits and prioritize by complexity, cycles, and namespace risk.
        </p>
      </section>

      <section className="surface projects-grid">
        <article className="project-card">
          <span className="project-label">Current assessment</span>
          <strong>{report?.projectName ?? "No project selected"}</strong>
          <p>{report ? `${report.moduleCount} modules • ${report.dependencyEdgeCount} dependencies` : "Run an assessment on Dashboard first."}</p>
        </article>
        <article className="project-card">
          <span className="project-label">Readiness</span>
          <strong>{report ? `${Math.max(0, 100 - report.complexityScore)}%` : "—"}</strong>
          <p>Higher values mean simpler migration and fewer blockers.</p>
        </article>
        <article className="project-card">
          <span className="project-label">Estimated budget</span>
          <strong>{report ? `$${report.priceEstimateUsd.toLocaleString("en-US")}` : "—"}</strong>
          <p>Derived from parser-based estimate outputs.</p>
        </article>
      </section>
    </section>
  );
}
