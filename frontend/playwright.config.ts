import { defineConfig, devices } from '@playwright/test'

const chromiumExecutablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.PLAYWRIGHT_MCP_EXECUTABLE_PATH

export default defineConfig({
  testDir: './tests',
  use: {
    ...devices['Desktop Chrome'],
    headless: true,
  },
  projects: [
    {
      name: 'chromium',
      use: {
        browserName: 'chromium',
        launchOptions: chromiumExecutablePath
          ? { executablePath: chromiumExecutablePath }
          : undefined,
      },
    },
    {
      name: 'firefox',
      use: {
        browserName: 'firefox',
      },
    },
  ],
})
