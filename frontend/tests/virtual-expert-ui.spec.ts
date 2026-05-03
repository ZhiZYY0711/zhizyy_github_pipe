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

  await expect(page.getByRole('button', { name: '收起' })).toBeVisible()
  await expect(page.getByRole('button', { name: '隐藏' })).toBeVisible()
  await expect(page.getByRole('button', { name: '新对话' })).toBeVisible()
  await expect(page.getByRole('button', { name: '搜索' })).toBeVisible()
  await expect(page.getByRole('button', { name: '归档' })).toBeVisible()
  await expect(page.getByRole('button', { name: '偏好' })).toBeVisible()
  await expect(page.getByText('置顶')).toBeVisible()
  await expect(page.getByRole('button', { name: /打开会话菜单/ })).toHaveText('更多')

  await expect(page.getByLabel('选择模型挡位')).toBeAttached()
  await expect(page.getByRole('button', { name: '语音输入' })).toHaveText('语音')
  await expect(page.getByRole('button', { name: '上传图片' })).toHaveText('图片')
  await expect(page.getByRole('button', { name: '发送' })).toHaveText('发送')

  const pageText = await page.locator('.agent-sidebar').innerText()
  expect(pageText).not.toMatch(/[☰‹›＋⌕▤⚙⌃⋯↩]/)
})
