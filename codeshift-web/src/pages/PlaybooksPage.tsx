const playbooks = [
  {
    title: "Service-first migration",
    description: "Start from leaf modules and move upward using the translation order.",
    steps: "Validate DTOs, migrate services, then expose new endpoints.",
  },
  {
    title: "Namespace modernization",
    description: "Handle javax usage upfront to reduce regressions in later stages.",
    steps: "Create compatibility layer, run package remap, verify integration tests.",
  },
  {
    title: "Cycle reduction sprint",
    description: "Break cyclic dependencies before major platform rewrites.",
    steps: "Extract shared interfaces and isolate side-effect modules.",
  },
];

export default function PlaybooksPage() {
  return (
    <section className="page-stack">
      <section className="surface">
        <p className="eyebrow">Playbooks</p>
        <h1>Out-of-the-box migration execution guides.</h1>
        <p className="surface-note">
          Pair assessment metrics with practical playbooks so teams can move from insight to action.
        </p>
      </section>

      <section className="surface projects-grid">
        {playbooks.map((item) => (
          <article key={item.title} className="project-card">
            <span className="project-label">{item.title}</span>
            <strong>{item.description}</strong>
            <p>{item.steps}</p>
          </article>
        ))}
      </section>
    </section>
  );
}
