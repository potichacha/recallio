import { readFile } from 'node:fs/promises'
import { signIn, USERS } from './support/auth'
import { expect, test } from './support/test'

test.describe('Mes données (ADR 0056)', () => {
  test('downloads the data of the account, then deletes it for good', async ({ page }) => {
    await signIn(page, USERS.dave, '/profile/data')
    const data = page.getByRole('region', { name: 'Mes données' })

    const download = page.waitForEvent('download')
    await data.getByRole('button', { name: 'Télécharger mes données' }).click()
    const file = await download
    expect(file.suggestedFilename()).toBe('recallio-export.json')
    const exported = JSON.parse(await readFile(await file.path(), 'utf8'))
    expect(exported.sections.user.app_user[0].display_name).toBe(USERS.dave.displayName)
    expect(Object.keys(exported.sections)).toContain('review')

    await data.getByRole('button', { name: 'Supprimer mon compte' }).click()
    const confirm = data.getByRole('button', { name: 'Supprimer définitivement' })
    await expect(confirm).toBeDisabled()
    await data.getByLabel(/Pour confirmer, écris ton nom/).fill(USERS.dave.displayName)
    await confirm.click()

    // Le compte applicatif est effacé ; Keycloak fait confirmer la suppression de l'identité elle-même.
    // Selon l'ancienneté de la connexion, il redemande d'abord le mot de passe.
    const password = page.locator('#password')
    const confirmDeletion = page.getByRole('button', { name: 'Confirm deletion' })
    await expect(password.or(confirmDeletion)).toBeVisible()
    if (await password.isVisible()) {
      await password.fill(USERS.dave.password)
      await page.locator('#kc-login').click()
    }
    await confirmDeletion.click()
    await expect(page.getByText('User deleted successfully')).toBeVisible()
    await page.getByRole('link', { name: /Back to Application/ }).click()
    await expect(page.getByRole('button', { name: 'Se connecter' }).first()).toBeVisible()

    // L'identité n'existe plus : la connexion est refusée.
    await page.getByRole('button', { name: 'Se connecter' }).first().click()
    await page.locator('#username').fill(USERS.dave.username)
    await page.locator('#password').fill(USERS.dave.password)
    await page.locator('#kc-login').click()
    await expect(page.getByText('Invalid username or password.')).toBeVisible()
  })
})
