import { signIn, USERS } from './support/auth'
import { expect, test } from './support/test'

test.describe('Thème (ADR 0058)', () => {
  test('follows the system, then keeps the theme chosen in the profile across reloads', async ({
    page,
  }) => {
    await signIn(page, USERS.bob, '/profile/settings')
    const html = page.locator('html')
    const theme = page.getByRole('group', { name: 'Thème' })
    // Le navigateur des parcours est en clair, et rien n'est encore choisi.
    await expect(html).toHaveAttribute('data-theme', 'light')
    await expect(theme.getByRole('radio', { name: 'Comme mon appareil' })).toBeChecked()

    await theme.getByRole('radio', { name: 'Sombre' }).check()
    await expect(html).toHaveAttribute('data-theme', 'dark')

    // Le script de la page pose le thème avant l'application : il tient après un rechargement.
    await page.reload()
    await expect(html).toHaveAttribute('data-theme', 'dark')
    await expect(theme.getByRole('radio', { name: 'Sombre' })).toBeChecked()

    await theme.getByRole('radio', { name: 'Comme mon appareil' }).check()
    await expect(html).toHaveAttribute('data-theme', 'light')
  })
})
