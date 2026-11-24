/// <reference types="vitest/config" />
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  // '' also loads variables without VITE_, like BACKEND_URL (dev server only, never sent to the browser)
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    resolve: {
      // must match "paths" in tsconfig.json
      alias: { '@': '/src' },
    },
    server: {
      port: 5173,
      // the backend's CORS allow-list contains exactly http://localhost:5173
      strictPort: true,
      proxy: {
        '/api': env.BACKEND_URL || 'http://localhost:8080',
      },
    },
    // same port as the dev server, for the same CORS reason
    preview: {
      port: 5173,
      strictPort: true,
    },
    test: {
      environment: 'jsdom',
      globals: true,
      setupFiles: ['./src/testing/setup-tests.ts'],
      include: ['src/**/*.test.{ts,tsx}'], // Playwright specs in tests/ are not Vitest tests
      css: false,
      coverage: {
        provider: 'v8',
        include: ['src/**/*.{ts,tsx}'],
        exclude: ['src/**/*.test.{ts,tsx}', 'src/**/__tests__/**', 'src/testing/**', 'src/main.tsx'],
        reporter: ['text-summary', 'text', 'html'],
        thresholds: { lines: 80, statements: 80, functions: 80, branches: 80 },
      },
    },
  };
});
