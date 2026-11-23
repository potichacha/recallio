import { expect, test } from './support/test'
import { signIn, USERS } from './support/auth'

test.describe('Connexion', () => {
  // Une seule image du front pour tous les environnements : nginx met l'adresse du fournisseur OIDC dans index.html à
  // son démarrage (ADR 0069). Sans elle, la PWA prendrait le Keycloak local, et les parcours de connexion passeraient
  // quand même : d'où ce test.
  test('serves the address of the identity provider in the page, filled in by nginx', async ({
    request,
  }) => {
    const html = await (await request.get('/')).text()

    expect(html).toContain(
      '<meta name="recallio-oidc-authority" content="http://localhost:8180/realms/recallio" />',
    )
    expect(html).not.toMatch(/__[A-Z_]+__/)
  })

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
    await page
      .getByRole('navigation', { name: 'Sections du profil' })
      .getByRole('link', { name: 'Mes données' })
      .click()
    await page.getByRole('button', { name: 'Se déconnecter' }).click()

    await expect(page.getByRole('button', { name: 'Se connecter' })).toBeVisible()
    await expect(page.getByRole('link', { name: USERS.bob.displayName })).toBeHidden()
  })
})
