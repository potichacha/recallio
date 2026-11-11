import { expect, test } from './support/test'
import { signIn, USERS } from './support/auth'

test.describe('Connexion', () => {
  test('asks for sign-in on a protected page, then comes back to it', async ({ page }) => {
    await page.goto('/profile')
    await expect(page.getByRole('heading', { name: 'Connexion requise' })).toBeVisible()

    await signIn(page, USERS.alice, '/profile')

    await expect(page).toHaveURL(/\/profile$/)
    await expect(page.getByRole('heading', { name: 'Mon profil' })).toBeVisible()
    await expect(page.getByText('Tu es administrateur')).toBeVisible()
  })

  test('signs out from the profile, through the identity provider', async ({ page }) => {
    await signIn(page, USERS.bob)

    await page.getByRole('link', { name: USERS.bob.displayName }).click()
    await page.getByRole('button', { name: 'Se déconnecter' }).click()

    await expect(page.getByRole('button', { name: 'Se connecter' })).toBeVisible()
    await expect(page.getByRole('link', { name: USERS.bob.displayName })).toBeHidden()
  })
})
