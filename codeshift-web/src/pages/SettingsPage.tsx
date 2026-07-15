interface SettingsPageProps {
  email: string;
  theme: "light" | "dark";
  onToggleTheme: () => void;
}

export default function SettingsPage({ email, theme, onToggleTheme }: SettingsPageProps) {
  return (
    <section className="page-stack">
      <section className="surface">
        <p className="eyebrow">Settings</p>
        <h1>Account and interface preferences.</h1>
        <p className="surface-note">Control your session and visual mode in one place.</p>
      </section>

      <section className="surface projects-grid">
        <article className="project-card">
          <span className="project-label">Account</span>
          <strong>{email}</strong>
          <p>You are signed in and can access all workspace routes.</p>
        </article>
        <article className="project-card">
          <span className="project-label">Appearance</span>
          <strong>{theme === "dark" ? "Dark mode" : "Light mode"}</strong>
          <p>Toggle between themes based on context and accessibility preference.</p>
          <button className="button button-secondary settings-button" onClick={onToggleTheme}>
            Switch to {theme === "dark" ? "light" : "dark"} mode
          </button>
        </article>
      </section>
    </section>
  );
}
