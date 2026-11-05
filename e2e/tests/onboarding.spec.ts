import { expect, test } from '@playwright/test'
import { signIn, subjectCheckbox, USERS } from './support/auth'

test.describe('Premier login', () => {
  test('chooses the subjects and the daily time, then reviews at once', async ({ page }) => {
    await signIn(page, USERS.carol)

    await expect(
      page.getByRole('heading', { name: 'Quels sujets veux-tu réviser ?' }),
    ).toBeVisible()
    const start = page.getByRole('button', { name: 'Commencer à réviser' })
    await expect(start).toBeDisabled()
    await page.getByRole('checkbox', { name: subjectCheckbox('Go') }).check()
    await page
      .getByRole('group', { name: 'Combien de temps par jour ?' })
      .getByText('5 min', { exact: true })
      .click()
    await start.click()

    // La première session part aussitôt, sur le sujet choisi, pour le temps choisi (ADR 0050).
    await expect(page).toHaveURL(/\/review\?subject=go&minutes=5&mode=all$/)
    await expect(page.getByRole('timer', { name: 'Temps restant' })).toHaveText('Encore 5 min')
    await expect(page.locator('article header')).toContainText('Go ·')
    await page.getByRole('button', { name: 'Voir la réponse' }).click()
  })
})
