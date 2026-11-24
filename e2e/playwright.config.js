// @ts-check
const { defineConfig, devices } = require('@playwright/test');
require('dotenv').config();

const frontendPort = process.env.E2E_FRONTEND_PORT || '5173';
const baseURL = process.env.BASE_URL || `http://localhost:${frontendPort}`;
const enableWebkit = process.env.E2E_ENABLE_WEBKIT === '1';

module.exports = defineConfig({
  testDir: './tests',

  timeout: 30000,
  expect: {
    timeout: 10_000,
  },

  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 1 : undefined,

  reporter: [
    ['list'],
    ['html', { open: 'never' }],
    ['json', { outputFile: 'test-results/results.json' }],
  ],

  use: {
    baseURL,
    headless: true,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
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
      testIgnore: '**/mobile-*.spec.js',
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
    ...(enableWebkit
      ? [
          {
            name: 'mobile-webkit',
            testMatch: ['**/smoke.spec.js', '**/mobile-*.spec.js'],
            use: {
              ...devices['iPhone 13'],
            },
          },
        ]
      : []),
  ],
});
