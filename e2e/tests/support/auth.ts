import { expect, type Page } from '@playwright/test'

/** Comptes du royaume de test (infra/keycloak/recallio-realm.json). */
export const USERS = {
  alice: { username: 'alice', password: 'alice', displayName: 'Alice Martin' },
  bob: { username: 'bob', password: 'bob', displayName: 'Bob Durand' },
} as const

export type TestUser = (typeof USERS)[keyof typeof USERS]

/**
 * Se connecte comme un vrai utilisateur : bouton « Se connecter », formulaire Keycloak, retour dans la PWA
 * (Authorization Code + PKCE). Les jetons étant en sessionStorage, chaque test démarre déconnecté.
 */
export async function signIn(page: Page, user: TestUser, startPath = '/') {
  await page.goto(startPath)
  await page.getByRole('button', { name: 'Se connecter' }).first().click()

  await page.locator('#username').fill(user.username)
  await page.locator('#password').fill(user.password)
  await page.locator('#kc-login').click()

  await expect(page.getByRole('link', { name: user.displayName })).toBeVisible()
}
