import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    allowedHosts: ["weddingmaster-fe.onrender.com"],
    port: 5173,
    proxy: {
      "/api": { target: "https://weddingmaster.onrender.com", changeOrigin: true },
      "/uploads": { target: "https://weddingmaster.onrender.com", changeOrigin: true },
      "/ws": { target: "https://weddingmaster.onrender.com", ws: true },
    },
  },
});
