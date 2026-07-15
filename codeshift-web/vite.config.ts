import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Dev server proxies API calls to the Spring Boot backend on :8080.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/public": "http://localhost:8080",
      "/runs": "http://localhost:8080",
      "/health": "http://localhost:8080",
    },
  },
});
