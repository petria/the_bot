import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';

const resolveModuleFile = (path: string) => fileURLToPath(new URL(path, import.meta.url));

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      'react-router/dom': resolveModuleFile('node_modules/react-router/dist/development/dom-export.mjs'),
      'react-remove-scroll': resolveModuleFile('node_modules/react-remove-scroll/dist/es2015/index.js'),
      'react-remove-scroll-bar/constants': resolveModuleFile(
        'node_modules/react-remove-scroll-bar/dist/es2015/constants.js',
      ),
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
