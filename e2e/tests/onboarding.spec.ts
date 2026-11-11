import { expect, test } from './support/test'
import { signIn, USERS } from './support/auth'

test.describe('Premier login', () => {
  test('tells their goal, chooses a subject, then reviews it for the chosen length', async ({
    page,
  }) => {
    await signIn(page, USERS.carol)

    // Le but règle la première étape (ADR 0052) : qui prépare un entretien part réviser.
    await expect(page.getByRole('heading', { name: 'Qu’est-ce qui t’amène ?' })).toBeVisible()
    const start = page.getByRole('button', { name: 'C’est parti' })
    await expect(start).toBeDisabled()
    await page.getByRole('group', { name: 'Ton but' }).getByText('Je prépare un entretien').click()
    await page
      .getByRole('group', { name: 'Sur quel sujet ?' })
      .getByText('Go', { exact: true })
      .click()
    await start.click()

    await expect(page.getByRole('heading', { name: 'Réviser Go' })).toBeVisible()
    await page.getByRole('group', { name: 'Durée' }).getByText('5 min', { exact: true }).click()
    await page.getByRole('button', { name: 'Commencer' }).click()
    await expect(page.getByRole('timer', { name: 'Temps restant' })).toHaveText('Encore 5 min')
    await expect(page.locator('article header')).toContainText('Go ·')
    await page.getByRole('button', { name: 'Voir la réponse' }).click()
  })
})
