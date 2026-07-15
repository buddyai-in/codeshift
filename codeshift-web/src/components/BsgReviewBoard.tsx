import { useState } from "react";
import type { BsgGraph, BsgNode, HumanStatus } from "../api";

interface Props {
  bsg: BsgGraph;
  onReview: (nodeRef: string, patch: { status?: HumanStatus; title?: string; description?: string }) => void;
  busy: boolean;
}

function NodeCard({ node, onReview, busy }: { node: BsgNode; onReview: Props["onReview"]; busy: boolean }) {
  const [title, setTitle] = useState(node.title);
  const [description, setDescription] = useState(node.description);
  const edited = title !== node.title || description !== node.description;

  return (
    <article className={`bsg-card bsg-card-${node.humanStatus.toLowerCase()}`}>
      <div className="bsg-card-head">
        <span className="bsg-ref">{node.nodeRef}</span>
        <span className="bsg-type">{node.nodeType.replaceAll("_", " ")}</span>
        <span className={`bsg-confidence bsg-confidence-${node.confidence.toLowerCase()}`}>
          {node.confidence}
        </span>
        <span className={`bsg-status bsg-status-${node.humanStatus.toLowerCase()}`}>
          {node.humanStatus}
        </span>
      </div>

      <input
        className="bsg-title-input"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        disabled={busy}
      />
      <textarea
        className="bsg-desc-input"
        value={description}
        rows={2}
        onChange={(e) => setDescription(e.target.value)}
        disabled={busy}
      />
      {node.sourceLocation && <div className="bsg-source">{node.sourceLocation}</div>}

      <div className="bsg-card-actions">
        <button
          className="button button-primary bsg-btn"
          disabled={busy}
          onClick={() => onReview(node.nodeRef, { status: "APPROVED", title, description })}
        >
          {edited ? "Save & approve" : "Approve"}
        </button>
        <button
          className="button button-secondary bsg-btn"
          disabled={busy}
          onClick={() => onReview(node.nodeRef, { status: "REJECTED" })}
        >
          Reject
        </button>
        {edited && (
          <button
            className="button button-ghost bsg-btn"
            disabled={busy}
            onClick={() => onReview(node.nodeRef, { status: "MODIFIED", title, description })}
          >
            Save edit
          </button>
        )}
      </div>
    </article>
  );
}

export default function BsgReviewBoard({ bsg, onReview, busy }: Props) {
  const counts = bsg.nodes.reduce(
    (acc, n) => ((acc[n.humanStatus] = (acc[n.humanStatus] ?? 0) + 1), acc),
    {} as Record<string, number>,
  );

  return (
    <div className="bsg-review">
      <div className="bsg-summary">
        <span className="bsg-pill bsg-pill-pending">{counts.PENDING ?? 0} pending</span>
        <span className="bsg-pill bsg-pill-approved">{counts.APPROVED ?? 0} approved</span>
        <span className="bsg-pill bsg-pill-modified">{counts.MODIFIED ?? 0} edited</span>
        <span className="bsg-pill bsg-pill-rejected">{counts.REJECTED ?? 0} rejected</span>
      </div>

      <div className="bsg-board">
        {bsg.nodes.map((n) => (
          <NodeCard key={n.nodeRef} node={n} onReview={onReview} busy={busy} />
        ))}
      </div>
    </div>
  );
}
