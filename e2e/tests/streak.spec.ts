import { expect, test } from '@playwright/test'
import { signIn, USERS } from './support/auth'

/** L'objectif quotidien vaut 3 cartes dans la pile de test (infra/compose.e2e.yaml). */
const DAILY_GOAL = 3

test.describe('Streak', () => {
  test('starts a streak once the daily goal is reached', async ({ page }) => {
    await signIn(page, USERS.alice, '/review')

    const reached = page.getByRole('status').filter({ hasText: 'Objectif atteint' })
    for (let review = 0; review < DAILY_GOAL && !(await reached.isVisible()); review++) {
      await page.getByRole('button', { name: 'Voir la réponse' }).click()
      await page
        .getByRole('group', { name: 'Noter la carte' })
        .getByRole('button', { name: 'Bien' })
        .click()
      await expect(page.getByText(/\d+ cartes? révisées?/)).toContainText(String(review + 1))
    }

    await expect(reached).toHaveText('Objectif atteint ! Votre série passe à 1 jour.')
    await expect(page.getByLabel('Série : 1 jour')).toBeVisible()

    await page.getByRole('link', { name: 'Recallio' }).click()
    await expect(page.getByText('Série', { exact: true }).locator('..')).toContainText('🔥 1 jour')
    // Les derniers jours : seul aujourd'hui a l'objectif atteint.
    await expect(page.getByLabel(/ : objectif atteint$/)).toHaveCount(1)
  })
})
