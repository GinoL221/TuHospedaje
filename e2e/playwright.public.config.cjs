// @ts-check
const path = require('node:path');
const { defineConfig } = require('@playwright/test');

const artifacts = process.env.PUBLIC_ARTIFACTS_DIR;
if (!artifacts || !path.isAbsolute(artifacts) || artifacts !== '/artifacts') {
  throw new Error('PUBLIC_ARTIFACTS_DIR must be /artifacts');
}

module.exports = defineConfig({
  testDir: './tests',
  testMatch: [
    '**/smoke.spec.js',
    '**/search.spec.js',
    '**/home-recommendations.spec.js',
  ],
  timeout: 30000,
  fullyParallel: false,
  workers: 1,
  reporter: [['list']],
  outputDir: '/artifacts/test-results',
  use: {
    baseURL: 'http://localhost:5173',
    headless: true,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'off',
    viewport: { width: 1280, height: 720 },
    animations: 'disabled',
  },
  projects: [
    { name: 'chromium', use: { browserName: 'chromium' } },
    { name: 'firefox', use: { browserName: 'firefox' } },
    { name: 'webkit', use: { browserName: 'webkit' } },
  ],
});
