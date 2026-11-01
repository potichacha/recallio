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
})
