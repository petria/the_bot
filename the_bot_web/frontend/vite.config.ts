import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/web': 'http://localhost:8091',
      '/login': 'http://localhost:8091',
      '/logout': 'http://localhost:8091',
    },
  },
});
