import { expect, test } from '@playwright/test'
import { followSubjects, signIn, USERS } from './support/auth'

test.describe('Session de révision', () => {
  test('reviews one subject for a chosen length, flipping self-assessed cards @mobile', async ({
    page,
  }) => {
    await signIn(page, USERS.bob, '/profile')
    await followSubjects(page, ['Java', 'Go'])
    await page.goto('/review')

    // Un sujet à la fois, pour une durée choisie (ADR 0048).
    await page.getByRole('group', { name: 'Sujet' }).getByText('Java', { exact: true }).click()
    await page.getByRole('group', { name: 'Durée' }).getByText('5 min', { exact: true }).click()
    await page.getByRole('button', { name: 'Commencer' }).click()
    await expect(page.getByRole('heading', { name: 'Réviser : Java' })).toBeVisible()
    await expect(page.getByRole('timer', { name: 'Temps restant' })).toHaveText('Encore 5 min')

    const answers = page.getByRole('group', { name: 'Connaissiez-vous la réponse ?' })
    for (const [index, answer] of ['Je savais', 'Je ne savais pas'].entries()) {
      await expect(page.locator('article header')).toContainText('Java ·')
      await page.getByRole('button', { name: 'Voir la réponse' }).click()
      await answers.getByRole('button', { name: answer }).click()
      await expect(
        page.getByText(`${index + 1} carte${index > 0 ? 's' : ''} révisée`),
      ).toBeVisible()
    }

    await page.getByRole('button', { name: 'Terminer la session' }).click()
    await expect(page.getByRole('region', { name: 'Bilan de la session' })).toContainText(
      '2 cartes révisées, dont 1 réussie.',
    )

    await page.getByRole('link', { name: 'Recallio' }).click()
    const subjects = page.getByRole('region', { name: 'Progression par sujet' })
    await expect(subjects.getByRole('listitem').first()).toContainText(/\d+ \/ \d+ vues/)
    await expect(page.getByLabel(/Série : \d+ jours?/)).toBeVisible()
  })

  test('reviews a single theme from its page', async ({ page }) => {
    await signIn(page, USERS.bob, '/subjects')
    await page
      .getByRole('region', { name: 'Langages' })
      .getByRole('link', { name: /^Java/ })
      .click()
    await page.getByRole('link', { name: /Bases du langage/ }).click()

    await page.getByRole('link', { name: 'Réviser ce thème' }).click()

    await expect(page).toHaveURL(/\/review\?theme=/)
    // L'URL change avant l'affichage de la page : on attend la page de révision elle-même.
    await expect(page.getByRole('heading', { name: 'Réviser', exact: true })).toBeVisible()
    await expect(page.locator('article header')).toContainText('Bases du langage')
    await page.getByRole('button', { name: 'Voir la réponse' }).click()
    await expect(page.getByRole('group', { name: 'Connaissiez-vous la réponse ?' })).toBeVisible()
  })
})
