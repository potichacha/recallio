import { expect, test, type Page } from '@playwright/test'
import { signIn, subjectCheckbox, USERS } from './support/auth'

/** Alice crée un groupe sur Go et y fait entrer Bob par un lien d'invitation. */
async function groupOfAliceAndBob(alice: Page, bob: Page, name: string) {
  await signIn(alice, USERS.alice, '/groups')
  await alice.getByLabel('Nom du groupe').fill(name)
  await alice.getByRole('button', { name: 'Créer' }).click()
  const settings = alice.getByRole('region', { name: 'Réglages' })
  await settings.getByRole('button', { name: 'Modifier les réglages' }).click()
  await settings.getByRole('checkbox', { name: subjectCheckbox('Go') }).check()
  await settings.getByRole('button', { name: 'Enregistrer les réglages' }).click()
  await expect(settings.getByRole('button', { name: 'Modifier les réglages' })).toBeVisible()

  await alice.getByRole('button', { name: "Créer un lien d'invitation" }).click()
  const link = await alice.getByLabel("Lien d'invitation").inputValue()
  await signIn(bob, USERS.bob, new URL(link).pathname)
  await bob.getByRole('button', { name: 'Rejoindre le groupe' }).click()
  await expect(bob.getByRole('heading', { name })).toBeVisible()
}

/** Plus long que la correction (5 s, recallio.live.reveal-seconds) qui précède chaque question, et la fin. */
const NEXT_QUESTION_TIMEOUT_MS = 10_000

test.describe('Mode live', () => {
  test('alice opens a live game, bob joins, they answer every question together, then see the podium', async ({
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

    // Alice voit Bob arriver sans recharger la page.
    const players = alice.getByRole('region', { name: /Joueurs/ })
    await expect(players).toContainText(USERS.bob.displayName)

    // Alice démarre : trois QCM de Go (la pile e2e limite la partie à trois questions), posés aux deux joueurs en même
    // temps.
    await alice.getByRole('button', { name: 'Démarrer la partie' }).click()
    for (const question of [1, 2, 3]) {
      for (const page of [alice, bob]) {
        // La question suivante vient après les 5 secondes de correction : on l'attend plus longtemps.
        await expect(page.getByText(`Question ${question} / 3`)).toBeVisible({
          timeout: NEXT_QUESTION_TIMEOUT_MS,
        })
        await page.locator('input[name="choice"]').first().check()
        await page.getByRole('button', { name: 'Valider' }).click()
      }
      // Les deux ont répondu : la correction arrive sans attendre la fin du chrono.
      for (const page of [alice, bob]) {
        await expect(page.getByRole('status')).toContainText(/sur 2 (a|ont) trouvé/)
      }
    }
    for (const page of [alice, bob]) {
      await expect(page.getByText('Partie terminée : merci d’avoir joué !')).toBeVisible({
        timeout: NEXT_QUESTION_TIMEOUT_MS,
      })
    }

    // Le podium réunit les deux joueurs ; le résultat reste visible depuis le groupe.
    const podium = alice.getByRole('region', { name: 'Podium' })
    await expect(podium).toContainText(USERS.alice.displayName)
    await expect(podium).toContainText(USERS.bob.displayName)
    await alice.getByRole('link', { name: '← Le groupe' }).click()
    const live = alice.getByRole('region', { name: 'Partie live' })
    await expect(live.getByText('Dernières parties')).toBeVisible()
    await expect(live).toContainText(USERS.bob.displayName)
  })

  test('when the host leaves the lobby, bob becomes the host and can start', async ({
    browser,
  }) => {
    const name = `Hôte ${Date.now()}`
    const alice = await (await browser.newContext()).newPage()
    const bob = await (await browser.newContext()).newPage()
    await groupOfAliceAndBob(alice, bob, name)

    await alice.getByRole('button', { name: 'Lancer une partie live' }).click()
    await expect(alice.getByRole('button', { name: 'Démarrer la partie' })).toBeVisible()
    await bob.reload()
    await bob.getByRole('link', { name: 'Rejoindre la partie' }).click()
    await expect(bob.getByText('En attente du lancement par l’hôte.')).toBeVisible()

    // Alice part : sans hôte, personne ne pourrait démarrer ; le rôle passe à Bob.
    await alice.goto('/groups')
    await expect(bob.getByRole('button', { name: 'Démarrer la partie' })).toBeVisible()
    await expect(bob.getByRole('region', { name: 'Joueurs (1)' })).toContainText('Hôte')
  })
})
