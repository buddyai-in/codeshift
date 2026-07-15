import { type FormEvent, useState } from "react";
import CodeShiftLogo from "../components/CodeShiftLogo";

interface LoginPageProps {
  onLogin: (email: string, password: string) => string | null;
  theme: "light" | "dark";
  onToggleTheme: () => void;
}

export default function LoginPage({ onLogin, theme, onToggleTheme }: LoginPageProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const maybeError = onLogin(email, password);
    setError(maybeError ?? "");
  }

  return (
    <div className="auth-shell">
      <div className="app-grid" />
      <div className="app-glow app-glow-a" />
      <div className="app-glow app-glow-b" />

      <div className="auth-card">
        <div className="auth-head">
          <div className="brand-lockup">
            <CodeShiftLogo className="brand-mark" />
            <div>
              <div className="brand">CodeShift</div>
              <p className="brand-copy">Sign in to your migration workspace.</p>
            </div>
          </div>
          <button className="button button-secondary" onClick={onToggleTheme}>
            {theme === "dark" ? "Light mode" : "Dark mode"}
          </button>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            placeholder="you@company.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            placeholder="Enter your password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          {error && <div className="error-banner">{error}</div>}

          <button className="button button-primary" type="submit">
            Login
          </button>
        </form>

        <div className="auth-note">
          <strong>What is included</strong>
          <span>Dashboard, project pages, playbooks, settings, 404 handling, and maintenance route.</span>
        </div>
      </div>
    </div>
  );
}
