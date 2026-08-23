// @ts-check
const { defineConfig } = require('@playwright/test');
require('dotenv').config();

module.exports = defineConfig({
  testDir: './tests',

  timeout: 30000,

  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,

  reporter: [['list'], ['html']],

  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:5173',
    headless: true,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'on-first-retry',
    viewport: { width: 1280, height: 720 },
    animations: 'disabled',
  },

  projects: [
    {
      name: 'chromium',
      testIgnore: '**/mobile-*.spec.js',
      use: { browserName: 'chromium' },
    },
    {
      name: 'firefox',
      // Visual baselines run on Chromium only: Firefox text AA is runner-sensitive
      // and doubles CI cost without proportional product signal (functional suite still runs).
      testIgnore: ['**/mobile-*.spec.js', '**/visual.spec.js'],
      use: { browserName: 'firefox' },
    },
    {
      name: 'mobile-chromium',
      testMatch: '**/mobile-*.spec.js',
      use: {
        browserName: 'chromium',
        viewport: { width: 390, height: 844 },
      },
    },
  ],
});
