import { useState } from "react";
import type { HardeningResult } from "../api";

type Tab = "dockerfile" | "kubernetesManifest" | "ciPipeline";
const TABS: { key: Tab; label: string }[] = [
  { key: "dockerfile", label: "Dockerfile" },
  { key: "kubernetesManifest", label: "Kubernetes" },
  { key: "ciPipeline", label: "CI pipeline" },
];

export default function HardeningView({ result }: { result: HardeningResult }) {
  const [tab, setTab] = useState<Tab>("dockerfile");

  return (
    <div className="xform-view">
      <section className="section-block">
        <div className="section-title-row">
          <h3>Security scan</h3>
          <span className="section-meta">
            {result.security.findings.length} findings · {result.security.highCount} high
          </span>
        </div>
        {result.security.findings.length === 0 ? (
          <p className="surface-note">No security signals found in the scanned source.</p>
        ) : (
          <div className="xform-tests">
            {result.security.findings.map((f, i) => (
              <div className="xform-test" key={i}>
                <span className={`bsg-status bsg-status-${f.severity === "HIGH" ? "rejected" : "modified"}`}>
                  {f.severity}
                </span>
                <span>{f.message}</span>
                <code className="section-meta">{f.location}</code>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="section-block">
        <div className="section-title-row">
          <h3>Deployment bundle</h3>
        </div>
        <div className="hard-tabs">
          {TABS.map((t) => (
            <button
              key={t.key}
              className={`hard-tab ${tab === t.key ? "hard-tab-active" : ""}`}
              onClick={() => setTab(t.key)}
            >
              {t.label}
            </button>
          ))}
        </div>
        <pre className="xform-code">{result.devops[tab]}</pre>
      </section>

      {result.messaging.topics.length > 0 && (
        <section className="section-block">
          <div className="section-title-row">
            <h3>MQ → Kafka plan</h3>
            <span className="section-meta">from {result.messaging.sourceSystems.join(", ")}</span>
          </div>
          <div className="arch-table">
            {result.messaging.topics.map((t) => (
              <div className="arch-row" key={t.name}>
                <code className="arch-module">{t.name}</code>
                <span className="arch-layer arch-layer-messaging">{t.partitions} partitions</span>
                <code className="arch-target">
                  key={t.partitionKey} · {t.consumerGroup}
                </code>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
