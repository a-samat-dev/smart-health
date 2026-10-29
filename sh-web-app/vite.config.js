import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // The gateway allows this origin in its CORS settings, so the port must not drift.
  server: { port: 5173, strictPort: true },
});
