import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import type { ProxyOptions } from "vite";
import type { IncomingMessage } from "node:http";

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? "http://localhost:8080";

// Dev server proxies API calls to the Spring Boot backend.
//
// Several client-side page routes share a prefix with an API path (e.g. the
// /billing, /compliance, /portfolio, /projects, /datashift *pages* vs the
// same-named API endpoints). A browser navigation and a fetch() to those paths
// are indistinguishable by URL, so we route by intent: requests that accept HTML
// (navigations / hard refreshes) are served the SPA; everything else (fetch, which
// sends Accept: */* or application/json) is proxied to the API on :8080.
function apiProxy(): ProxyOptions {
  return {
    target: apiProxyTarget,
    changeOrigin: true,
    bypass(req: IncomingMessage) {
      const accept = req.headers.accept ?? "";
      if (req.method === "GET" && accept.includes("text/html")) {
        return req.url; // let Vite serve the SPA (history fallback -> index.html)
      }
      return undefined; // proxy to the backend API
    },
  };
}

// Every top-level backend route prefix. New backend prefixes must be added here.
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
    proxy: Object.fromEntries(backendPrefixes.map((prefix) => [prefix, apiProxy()])),
  },
});
