import { defineConfig, devices } from '@playwright/test';

/** End-to-end tests against the real backend. Both servers are started if needed, or reused if already running. */
export default defineConfig({
  testDir: './tests',
  // tests buy real stock in a shared backend: never in parallel
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: './mvnw spring-boot:run',
      cwd: '../backend',
      url: 'http://localhost:8080/api/v1/books?size=1',
      reuseExistingServer: true,
      timeout: 180_000,
    },
    {
      command: 'npm run dev',
      url: 'http://localhost:5173',
      reuseExistingServer: true,
      timeout: 60_000,
    },
  ],
});
