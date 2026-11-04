import { expect, test } from '@playwright/test'
import { signIn, USERS } from './support/auth'

/** Une exécution passe par le vrai runner et un conteneur du bac à sable Go (ADR 0037) : quelques secondes. */
const RUN_TIMEOUT = 30_000

test.describe('Le Tour', () => {
  test('reads the Go Tour without an account', async ({ page }) => {
    await page.goto('/subjects/go/tour')
    await expect(page.getByRole('heading', { name: 'Le Tour de Go' })).toBeVisible()

    await page.getByRole('link', { name: 'Commencer le Tour' }).click()

    await expect(page.getByRole('heading', { name: 'Bienvenue' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Se connecter pour exécuter' })).toBeVisible()
  })

  test('runs the program of a page in the sandbox, then goes to the next page', async ({
    page,
  }) => {
    await signIn(page, USERS.alice, '/subjects/go')
    await page.getByRole('link', { name: 'Le Tour', exact: true }).click()
    await page.getByRole('link', { name: 'Commencer le Tour' }).click()
    await expect(page.getByRole('heading', { name: 'Bienvenue' })).toBeVisible()

    await page.getByRole('button', { name: 'Exécuter' }).click()

    await expect(page.getByLabel('Sortie standard')).toContainText(
      'Bienvenue dans le Tour de Go !',
      { timeout: RUN_TIMEOUT },
    )
    await page.getByRole('link', { name: 'Page suivante' }).click()
    await expect(page.getByRole('heading', { name: 'Le premier programme' })).toBeVisible()
  })
})
