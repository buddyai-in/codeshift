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
    <section className="panel upload">
      <h2>Assess a codebase</h2>
      <p className="muted">
        Upload a <code>.zip</code> of your Java sources — get a dependency graph, effort estimate
        and price in seconds. No account.
      </p>

      <div className="row">
        <input
          type="file"
          accept=".zip"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          disabled={loading}
        />
        <input
          type="text"
          placeholder="Project name (optional)"
          value={name}
          onChange={(e) => setName(e.target.value)}
          disabled={loading}
        />
      </div>

      <div className="row">
        <button
          className="primary"
          disabled={loading || !file}
          onClick={() => file && run(() => assessZip(file, name))}
        >
          {loading ? "Assessing…" : "Assess upload"}
        </button>
        <button
          className="ghost"
          disabled={loading}
          onClick={() => run(() => assessPath(SAMPLE_PATH, "sample"))}
        >
          Try the sample project
        </button>
      </div>
    </section>
  );
}
