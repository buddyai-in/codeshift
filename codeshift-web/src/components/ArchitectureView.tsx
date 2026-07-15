import type { ArchitecturePlan } from "../api";

const layerClass = (layer: string) => `arch-layer arch-layer-${layer.toLowerCase()}`;

export default function ArchitectureView({ plan }: { plan: ArchitecturePlan }) {
  return (
    <div className="arch-view">
      <div className="arch-meta">
        <span className="bsg-pill bsg-pill-approved">{plan.targetStack.replaceAll("_", " ")}</span>
        <span className="section-meta">{plan.moduleMappings.length} modules</span>
        <span className="section-meta">{plan.microservices.length} service boundary(ies)</span>
        <span className="section-meta">{plan.phases.length} migration phases</span>
      </div>

      <section className="section-block">
        <div className="section-title-row">
          <h3>Module → class mapping</h3>
        </div>
        <div className="arch-table">
          {plan.moduleMappings.map((m) => (
            <div className="arch-row" key={m.moduleId}>
              <code className="arch-module">{m.moduleId}</code>
              <span className={layerClass(m.layer)}>{m.layer}</span>
              <code className="arch-target">{m.targetClass}</code>
            </div>
          ))}
        </div>
      </section>

      <section className="section-block">
        <div className="section-title-row">
          <h3>Migration phases (dependency order)</h3>
        </div>
        <ol className="arch-phases">
          {plan.phases.map((p) => (
            <li key={p.order}>
              <strong>{p.name}</strong>
              <span className="section-meta">{p.moduleIds.length} module(s)</span>
              <div className="arch-phase-mods">
                {p.moduleIds.map((id) => (
                  <code key={id}>{id.split(".").pop()}</code>
                ))}
              </div>
            </li>
          ))}
        </ol>
      </section>

      <section className="section-block">
        <div className="section-title-row">
          <h3>Proposed service boundaries</h3>
        </div>
        <div className="arch-boundaries">
          {plan.microservices.map((s) => (
            <div className="arch-boundary" key={s.name}>
              <strong>{s.name}</strong>
              <span className="section-meta">{s.moduleIds.length} module(s)</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
