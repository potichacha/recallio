import { expect, test, type Page } from '@playwright/test'
import { signIn, USERS } from './support/auth'

/** Alice crée un groupe sur Go et y fait entrer Bob par un lien d'invitation. */
async function groupOfAliceAndBob(alice: Page, bob: Page, name: string) {
  await signIn(alice, USERS.alice, '/groups')
  await alice.getByLabel('Nom du groupe').fill(name)
  await alice.getByRole('button', { name: 'Créer' }).click()
  const settings = alice.getByRole('region', { name: 'Réglages' })
  await settings.getByRole('button', { name: 'Modifier les réglages' }).click()
  await settings.getByRole('checkbox', { name: 'Go' }).check()
  await settings.getByRole('button', { name: 'Enregistrer les réglages' }).click()
  await expect(settings.getByRole('button', { name: 'Modifier les réglages' })).toBeVisible()

  await alice.getByRole('button', { name: "Créer un lien d'invitation" }).click()
  const link = await alice.getByLabel("Lien d'invitation").inputValue()
  await signIn(bob, USERS.bob, new URL(link).pathname)
  await bob.getByRole('button', { name: 'Rejoindre le groupe' }).click()
  await expect(bob.getByRole('heading', { name })).toBeVisible()
}

test.describe('Mode live', () => {
  test('alice opens a live game, bob joins its lobby, both see each other live', async ({
    browser,
  }) => {
    const name = `Live ${Date.now()}`
    const alice = await (await browser.newContext()).newPage()
    const bob = await (await browser.newContext()).newPage()
    await groupOfAliceAndBob(alice, bob, name)

    // Le WebSocket passe par nginx, authentifié par le jeton porté dans le sous-protocole.
    await alice.getByRole('button', { name: 'Lancer une partie live' }).click()
    await expect(alice.getByRole('heading', { name: 'Partie live : Go' })).toBeVisible()
    await expect(alice.getByRole('region', { name: 'Joueurs (1)' })).toContainText(
      `${USERS.alice.displayName}(vous)Hôte`,
    )

    await bob.reload()
    await bob.getByRole('link', { name: 'Rejoindre la partie' }).click()
    await expect(bob.getByText('En attente du lancement par l’hôte.')).toBeVisible()
    await expect(bob.getByRole('region', { name: 'Joueurs (2)' })).toBeVisible()

    // Alice voit Bob arriver, puis repartir, sans recharger la page.
    const players = alice.getByRole('region', { name: /Joueurs/ })
    await expect(players).toContainText(USERS.bob.displayName)
    await bob.goto('/groups')
    await expect(alice.getByRole('region', { name: 'Joueurs (1)' })).toBeVisible()
  })
})
