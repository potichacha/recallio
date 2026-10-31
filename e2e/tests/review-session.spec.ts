import { expect, test } from '@playwright/test'
import { signIn, USERS } from './support/auth'

test.describe('Session de révision', () => {
  test('reviews cards with self-assessment and sees the progress on the dashboard @mobile', async ({
    page,
  }) => {
    await signIn(page, USERS.bob, '/review')

    const ratings = page.getByRole('group', { name: 'Noter la carte' })
    for (const [index, rating] of ['Bien', 'À revoir'].entries()) {
      await page.getByRole('button', { name: 'Voir la réponse' }).click()
      await expect(ratings.getByRole('button', { name: rating })).toHaveText(
        /\d+ (min|h|j|mois|an)/,
      )
      await ratings.getByRole('button', { name: rating }).click()
      await expect(
        page.getByText(`${index + 1} carte${index > 0 ? 's' : ''} révisée`),
      ).toBeVisible()
    }

    await page.getByRole('link', { name: 'Recallio' }).click()
    const themes = page.getByRole('region', { name: 'Progression par thème' })
    await expect(themes.getByRole('listitem').first()).toContainText(/\d+ \/ \d+ vues/)
    await expect(page.getByLabel(/Série : \d+ jours?/)).toBeVisible()
  })

  test('reviews a single theme from its page', async ({ page }) => {
    await signIn(page, USERS.bob, '/subjects')
    await page
      .getByRole('region', { name: 'Langages' })
      .getByRole('link', { name: /^Java/ })
      .click()
    await page.getByRole('link', { name: /JAVA : FONDAMENTAUX/ }).click()

    await page.getByRole('link', { name: 'Réviser ce thème' }).click()

    await expect(page).toHaveURL(/\/review\?theme=/)
    // L'URL change avant l'affichage de la page : on attend la page de révision elle-même.
    await expect(page.getByRole('heading', { name: 'Réviser', exact: true })).toBeVisible()
    await page.getByRole('button', { name: 'Voir la réponse' }).click()
    await expect(page.getByRole('group', { name: 'Noter la carte' })).toBeVisible()
    await expect(page.locator('article header')).toContainText('JAVA : FONDAMENTAUX')
  })
})
