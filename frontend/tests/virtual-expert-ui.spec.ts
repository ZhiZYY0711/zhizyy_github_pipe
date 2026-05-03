import { expect, test } from '@playwright/test'

test('virtual expert sidebar and composer use text controls', async ({ page }) => {
  await page.route('**/api/manager/virtual-expert/agent/sessions?**', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        sessions: [
          {
            id: 'ana_test_001',
            title: '压力波动复核',
            status: 'completed',
            pinned: true,
            updatedAt: '2026-05-03T10:00:00+08:00',
          },
        ],
      }),
    })
  })
  await page.route('**/api/manager/virtual-expert/agent/memories?**', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify({ items: [] }) })
  })
  await page.route('**/api/manager/virtual-expert/agent/sessions/ana_test_001/timeline?**', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        sessionId: 'ana_test_001',
        items: [],
        hasMoreBefore: false,
      }),
    })
  })

  await page.addInitScript(() => {
    window.localStorage.setItem('token', 'dev-token')
    window.localStorage.setItem('jwt', 'dev-token')
    window.localStorage.setItem('username', 'tester')
    window.localStorage.setItem('isLoggedIn', 'true')
  })

  await page.goto(`${process.env.PW_BASE_URL || 'http://127.0.0.1:5173'}/virtual-expert`)

  // Sidebar toolbar buttons (icon+text)
  await expect(page.getByRole('button', { name: /收起列表|展开列表/ })).toBeVisible()
  await expect(page.getByRole('button', { name: '新对话' })).toBeVisible()
  await expect(page.getByRole('button', { name: '搜索' })).toBeVisible()
  await expect(page.getByRole('button', { name: '归档' })).toBeVisible()
  await expect(page.getByRole('button', { name: '偏好' })).toBeVisible()

  // Session card
  await expect(page.getByText('置顶')).toBeVisible()
  await expect(page.getByRole('button', { name: /打开会话菜单/ })).toHaveText('更多')

  // Composer — icon buttons with aria-labels
  await expect(page.getByLabel('选择模型挡位')).toBeAttached()
  await expect(page.getByLabel('语音输入')).toBeAttached()
  await expect(page.getByLabel('上传图片')).toBeAttached()
  await expect(page.getByLabel('发送')).toBeAttached()

  const pageText = await page.locator('.agent-sidebar').innerText()
  expect(pageText).not.toMatch(/[☰‹›＋⌕▤⚙⌃⋯↩]/)
})
