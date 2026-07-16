import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? "http://localhost:8080";

// Dev server proxies API calls to the Spring Boot backend. Every top-level backend
// route prefix must be listed here, otherwise the request hits the Vite dev server
// (:5173) and 404s instead of reaching the API (:8080).
const backendPrefixes = [
  "/public", // free assessment funnel
  "/runs", // migration runs + BSG review
  "/health",
  "/projects", // projects, versions, debt, performance, feature-requests, budget, compliance
  "/portfolio",
  "/orgs", // tenants
  "/tenants", // BYOK keys + model deployment
  "/bsg", // GET /bsg/versions/{id}
  "/billing", // usage, budget, invoice, checkout, payments, webhook
  "/compliance", // PCI-DSS / HIPAA packs
  "/datashift", // Oracle -> PostgreSQL DDL
  "/artifacts", // per-tenant object storage
];

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: Object.fromEntries(
      backendPrefixes.map((prefix) => [prefix, { target: apiProxyTarget, changeOrigin: true }]),
    ),
  },
});
