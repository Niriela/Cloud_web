import { reactRouter } from "@react-router/dev/vite";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from "vite";
import tsconfigPaths from "vite-tsconfig-paths";

export default defineConfig({
  plugins: [tailwindcss(), reactRouter(), tsconfigPaths()],
  server: {
    host: '0.0.0.0',  // Important pour Docker
    port: 5173,
    strictPort: true,
    hmr: {
      clientPort: 5173,
    },
    watch: {
      usePolling: true, // Nécessaire pour Docker sur certains systèmes
    },
  },
  build: {
    outDir: 'build/client',  // React Router utilise ce chemin
    sourcemap: false,
    emptyOutDir: true,
  },
  // Pour les variables d'environnement avec Vite
  define: {
    'import.meta.env.VITE_API_URL': JSON.stringify(process.env.VITE_API_URL || 'http://localhost:8083/api'),
    'import.meta.env.VITE_APP_NAME': JSON.stringify(process.env.VITE_APP_NAME || 'CloudWeb'),
  }
});