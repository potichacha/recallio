import { expect, test } from './support/test'

test.describe('États des écrans (ADR 0058)', () => {
  test('says when the network is gone, and stops when it comes back', async ({ page, context }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    const offline = page.getByRole('status').filter({ hasText: 'Tu es hors ligne' })
    await expect(offline).toBeHidden()

    await context.setOffline(true)
    await expect(offline).toBeVisible()

    await context.setOffline(false)
    await expect(offline).toBeHidden()
  })
})
