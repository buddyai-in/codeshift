import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <div className="status-page">
      <section className="surface status-card">
        <p className="eyebrow">404</p>
        <h1>This page does not exist.</h1>
        <p className="surface-note">Check the URL or return to a known route.</p>
        <div className="status-actions">
          <Link className="button button-primary" to="/">
            Go to dashboard
          </Link>
          <Link className="button button-secondary" to="/login">
            Go to login
          </Link>
        </div>
      </section>
    </div>
  );
}
