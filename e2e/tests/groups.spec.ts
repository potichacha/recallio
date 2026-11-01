import { expect, test } from '@playwright/test'
import { signIn, USERS } from './support/auth'

test.describe('Groupes', () => {
  test('creates a group, finds it among my groups, then deletes it', async ({ page }) => {
    // Un nom propre à ce passage : les tests restent rejouables sur une base déjà utilisée.
    const name = `Équipe ${Date.now()}`
    await signIn(page, USERS.alice, '/groups')

    await page.getByLabel('Nom du groupe').fill(name)
    await page.getByRole('button', { name: 'Créer' }).click()

    await expect(page.getByRole('heading', { name })).toBeVisible()
    await expect(page.getByRole('region', { name: 'Membres (1)' })).toContainText('Créateur')

    await page.getByRole('link', { name: '← Mes groupes' }).click()
    await page.getByRole('link', { name: new RegExp(name) }).click()
    await page.getByRole('button', { name: 'Supprimer le groupe' }).click()
    await page.getByRole('button', { name: 'Confirmer la suppression' }).click()

    await expect(page.getByRole('heading', { name: 'Mes groupes' })).toBeVisible()
    await expect(page.getByRole('link', { name: new RegExp(name) })).toBeHidden()
  })

  test('alice invites bob with a link, bob joins, then the replaced link stops working', async ({
    browser,
  }) => {
    const name = `Invitation ${Date.now()}`
    const alice = await (await browser.newContext()).newPage()
    const bob = await (await browser.newContext()).newPage()

    await signIn(alice, USERS.alice, '/groups')
    await alice.getByLabel('Nom du groupe').fill(name)
    await alice.getByRole('button', { name: 'Créer' }).click()
    await alice.getByRole('button', { name: "Créer un lien d'invitation" }).click()
    const link = await alice.getByLabel("Lien d'invitation").inputValue()

    // Bob ouvre le lien sans être connecté : la connexion le ramène à l'invitation.
    await signIn(bob, USERS.bob, new URL(link).pathname)
    await expect(bob.getByRole('heading', { name: `Rejoindre « ${name} »` })).toBeVisible()
    await bob.getByRole('button', { name: 'Rejoindre le groupe' }).click()
    await expect(bob.getByRole('region', { name: 'Membres (2)' })).toContainText(
      `${USERS.bob.displayName} (vous)`,
    )

    await alice.reload()
    await expect(alice.getByRole('region', { name: 'Membres (2)' })).toContainText(
      USERS.bob.displayName,
    )
    await alice.getByRole('button', { name: 'Nouveau lien' }).click()
    await expect(alice.getByLabel("Lien d'invitation")).not.toHaveValue(link)

    await bob.goto(new URL(link).pathname)
    await expect(bob.getByRole('heading', { name: 'Invitation invalide' })).toBeVisible()

    await alice.getByRole('button', { name: 'Supprimer le groupe' }).click()
    await alice.getByRole('button', { name: 'Confirmer la suppression' }).click()
    await expect(alice.getByRole('heading', { name: 'Mes groupes' })).toBeVisible()
  })
})
