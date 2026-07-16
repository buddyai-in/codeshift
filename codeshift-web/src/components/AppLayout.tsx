import { NavLink, Outlet } from "react-router-dom";
import CodeShiftLogo from "./CodeShiftLogo";

interface AppLayoutProps {
  email: string;
  loading: boolean;
  hasResult: boolean;
  theme: "light" | "dark";
  onToggleTheme: () => void;
  onLogout: () => void;
}

const navItems = [
  { to: "/", label: "Dashboard", end: true },
  { to: "/migrate", label: "Migrate", end: false },
  { to: "/new-code", label: "New code", end: false },
  { to: "/projects", label: "Projects", end: false },
  { to: "/playbooks", label: "Playbooks", end: false },
  { to: "/settings", label: "Settings", end: false },
] as const;

export default function AppLayout({
  email,
  loading,
  hasResult,
  theme,
  onToggleTheme,
  onLogout,
}: AppLayoutProps) {
  return (
    <div className="app-shell">
      <div className="app-grid" />
      <div className="app-glow app-glow-a" />
      <div className="app-glow app-glow-b" />

      <div className="app">
        <header className="shell-header">
          <div className="brand-lockup">
            <CodeShiftLogo className="brand-mark" />
            <div>
              <div className="brand">CodeShift</div>
              <p className="brand-copy">Multi-page migration workspace</p>
            </div>
          </div>

          <nav className="shell-nav" aria-label="Primary">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) => `shell-nav-link ${isActive ? "shell-nav-link-active" : ""}`}
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="shell-actions">
            <button className="button button-secondary shell-action" onClick={onToggleTheme}>
              {theme === "dark" ? "Light mode" : "Dark mode"}
            </button>
            <div className="status-chip">
              <span className={`status-dot ${loading ? "status-dot-loading" : "status-dot-ready"}`} />
              {loading ? "Analyzing" : hasResult ? "Ready" : "Idle"}
            </div>
          </div>
        </header>

        <section className="workspace">
          <aside className="rail">
            <section className="surface rail-panel">
              <p className="eyebrow">Navigation</p>
              <div className="rail-menu">
                {navItems.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) => `rail-menu-item ${isActive ? "rail-menu-item-active" : ""}`}
                  >
                    {item.label}
                  </NavLink>
                ))}
                <NavLink to="/maintenance" className="rail-menu-item">
                  Maintenance
                </NavLink>
              </div>
              <div className="rail-user">
                <strong>Signed in</strong>
                <span>{email}</span>
              </div>
              <button className="button button-secondary" onClick={onLogout}>
                Logout
              </button>
            </section>
          </aside>

          <main className="canvas">
            <Outlet />
          </main>
        </section>

        <footer className="shell-footer">
          <div>
            <strong>CodeShift</strong>
            <span>Dashboard, projects, playbooks, settings, auth, 404, and maintenance flows.</span>
          </div>
          <div className="shell-footer-links">
            <NavLink to="/">Dashboard</NavLink>
            <NavLink to="/projects">Projects</NavLink>
            <NavLink to="/settings">Settings</NavLink>
          </div>
        </footer>
      </div>
    </div>
  );
}
