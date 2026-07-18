/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    css: false,
    clearMocks: true,
    restoreMocks: true,
    forbidOnly: !!process.env.CI,
    // @vitest/coverage-v8 is pinned to the exact locked vitest version in
    // package.json (not ^) because this repo's npm is gated by npq-hero
    // (min-release-age=7d); a caret range can resolve to a too-recent patch
    // that gets blocked on install.
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      reportsDirectory: './coverage',
      include: ['src/**/*.{js,jsx}'],
      exclude: [
        'src/main.jsx',
        'src/test/**',
        'src/**/*.test.{js,jsx}',
      ],
      // Floor reflects measured coverage after Sprint 4 test additions
      // (85.34% statements / 80.12% branches / 74.88% functions / 87.79% lines
      // as of 2026-06). Any regression below these values fails CI.
      thresholds: {
        statements: 85,
        branches: 80,
        functions: 74,
        lines: 87,
      },
    },
  },
})
