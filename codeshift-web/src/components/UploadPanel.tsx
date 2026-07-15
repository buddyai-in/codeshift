import { useState } from "react";
import { assessPath, assessZip, type AssessmentResult } from "../api";

// The sample project bundled in the repo (path is relative to the running API's
// working dir, i.e. codeshift-api/ when launched with `mvn -pl codeshift-api spring-boot:run`).
const SAMPLE_PATH = "../codeshift-parser/src/test/resources/sample-project";

interface Props {
  onResult: (r: AssessmentResult) => void;
  onError: (msg: string) => void;
  onLoading: (loading: boolean) => void;
  loading: boolean;
}

export default function UploadPanel({ onResult, onError, onLoading, loading }: Props) {
  const [file, setFile] = useState<File | null>(null);
  const [name, setName] = useState("");

  async function run(fn: () => Promise<AssessmentResult>) {
    onError("");
    onLoading(true);
    try {
      onResult(await fn());
    } catch (e) {
      onError(e instanceof Error ? e.message : String(e));
    } finally {
      onLoading(false);
    }
  }

  return (
    <section className="surface upload-panel">
      <div className="surface-head stack">
        <div>
          <p className="eyebrow">Input</p>
          <h2>Assess a codebase</h2>
        </div>
        <p className="surface-note">
          Upload a <code>.zip</code> of your Java sources to generate the assessment in seconds.
        </p>
      </div>

      <div className="field">
        <label htmlFor="project-name">Project name</label>
        <input
          id="project-name"
          type="text"
          placeholder="Optional label for the report"
          value={name}
          onChange={(e) => setName(e.target.value)}
          disabled={loading}
        />
      </div>

      <div className="field">
        <label htmlFor="project-zip">Source zip</label>
        <input
          id="project-zip"
          type="file"
          accept=".zip"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          disabled={loading}
        />
        <p className="field-help">{file ? file.name : "No file selected yet."}</p>
      </div>

      <div className="action-row">
        <button
          className="button button-primary"
          disabled={loading || !file}
          onClick={() => file && run(() => assessZip(file, name))}
        >
          {loading ? "Analyzing…" : "Analyze upload"}
        </button>
        <button className="button button-secondary" disabled={loading} onClick={() => run(() => assessPath(SAMPLE_PATH, "sample"))}>
          Try sample project
        </button>
      </div>

      <div className="panel-footer">
        <span>Supports large archives</span>
        <span>Java source only</span>
      </div>
    </section>
  );
}
