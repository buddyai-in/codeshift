import { useEffect, useMemo, useState } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import type { AssessmentResult } from "./api";
import AppLayout from "./components/AppLayout";
import DashboardPage from "./pages/DashboardPage";
import LoginPage from "./pages/LoginPage";
import MigratePage from "./pages/MigratePage";
import NewCodePage from "./pages/NewCodePage";
import PortfolioPage from "./pages/PortfolioPage";
import MaintenancePage from "./pages/MaintenancePage";
import NotFoundPage from "./pages/NotFoundPage";
import PlaybooksPage from "./pages/PlaybooksPage";
import ProjectsPage from "./pages/ProjectsPage";
import SettingsPage from "./pages/SettingsPage";

type ThemeMode = "light" | "dark";
type Session = { email: string };

const SESSION_KEY = "codeshift-session";
const THEME_KEY = "codeshift-theme";

function readTheme(): ThemeMode {
  if (typeof window === "undefined") return "dark";
  const stored = window.localStorage.getItem(THEME_KEY);
  if (stored === "light" || stored === "dark") return stored;
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function readSession(): Session | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as Session;
    return parsed?.email ? parsed : null;
  } catch {
    return null;
  }
}

export default function App() {
  const [theme, setTheme] = useState<ThemeMode>(() => readTheme());
  const [session, setSession] = useState<Session | null>(() => readSession());
  const sessionEmail = session?.email ?? "";
  const [result, setResult] = useState<AssessmentResult | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
    window.localStorage.setItem(THEME_KEY, theme);
  }, [theme]);

  const toggleTheme = () => setTheme((prev) => (prev === "dark" ? "light" : "dark"));

  const authActions = useMemo(
    () => ({
      login: (email: string, password: string): string | null => {
        if (!email.trim()) return "Email is required.";
        if (!password.trim()) return "Password is required.";
        const nextSession = { email: email.trim().toLowerCase() };
        setSession(nextSession);
        window.localStorage.setItem(SESSION_KEY, JSON.stringify(nextSession));
        return null;
      },
      logout: () => {
        setSession(null);
        setResult(null);
        setError("");
        setLoading(false);
        window.localStorage.removeItem(SESSION_KEY);
      },
    }),
    [],
  );

  return (
    <Routes>
      <Route
        path="/login"
        element={
          session ? (
            <Navigate to="/" replace />
          ) : (
            <LoginPage onLogin={authActions.login} theme={theme} onToggleTheme={toggleTheme} />
          )
        }
      />

      <Route path="/maintenance" element={<MaintenancePage />} />

      <Route
        path="/"
        element={
          session ? (
            <AppLayout
              email={session.email}
              loading={loading}
              hasResult={result != null}
              theme={theme}
              onToggleTheme={toggleTheme}
              onLogout={authActions.logout}
            />
          ) : (
            <Navigate to="/login" replace />
          )
        }
      >
        <Route
          index
          element={
            <DashboardPage
              result={result}
              setResult={setResult}
              error={error}
              setError={setError}
              loading={loading}
              setLoading={setLoading}
            />
          }
        />
        <Route path="migrate" element={<MigratePage />} />
        <Route path="new-code" element={<NewCodePage />} />
        <Route path="portfolio" element={<PortfolioPage />} />
        <Route path="projects" element={<ProjectsPage report={result?.report ?? null} />} />
        <Route path="playbooks" element={<PlaybooksPage />} />
        <Route path="settings" element={<SettingsPage email={sessionEmail} theme={theme} onToggleTheme={toggleTheme} />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
