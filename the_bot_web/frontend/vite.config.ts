import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      'react-remove-scroll-bar/constants': 'react-remove-scroll-bar/dist/es2015/constants.js',
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api/web': 'http://localhost:8091',
      '/login': 'http://localhost:8091',
      '/logout': 'http://localhost:8091',
      '/default-ui.css': 'http://localhost:8091',
    },
  },
});
