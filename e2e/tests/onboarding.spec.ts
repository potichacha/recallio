import { expect, test } from '@playwright/test'
import { signIn, USERS } from './support/auth'

test.describe('Premier login', () => {
  test('chooses the subjects to review, then reviews them', async ({ page }) => {
    await signIn(page, USERS.carol)

    await expect(
      page.getByRole('heading', { name: 'Quels sujets voulez-vous réviser ?' }),
    ).toBeVisible()
    const start = page.getByRole('button', { name: 'Commencer à réviser' })
    await expect(start).toBeDisabled()
    await page.getByRole('checkbox', { name: 'Go' }).check()
    await start.click()

    await expect(page).toHaveURL(/\/review$/)
    await page.getByRole('button', { name: 'Voir la réponse' }).click()
    await expect(page.locator('article header')).toContainText('Go ·')
  })
})
