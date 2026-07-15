import { Link } from "react-router-dom";

export default function MaintenancePage() {
  return (
    <div className="status-page">
      <section className="surface status-card">
        <p className="eyebrow">Maintenance</p>
        <h1>We are improving CodeShift right now.</h1>
        <p className="surface-note">
          The workspace is temporarily under maintenance. Core analysis endpoints may be unavailable for a short period.
        </p>
        <div className="status-actions">
          <Link className="button button-primary" to="/">
            Back to dashboard
          </Link>
          <Link className="button button-secondary" to="/login">
            Login page
          </Link>
        </div>
      </section>
    </div>
  );
}
