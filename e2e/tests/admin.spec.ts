import { expect, test } from './support/test'
import { signIn, USERS } from './support/auth'
import { seriousViolations } from './support/axe'

test.describe('Page « Bêta » des admins', () => {
  // La page est ouverte contre le vrai rapport (ADR 0076) : ses tests unitaires ne voient que des réponses simulées.
  for (const scheme of ['light', 'dark'] as const) {
    test(`shows the report to an admin, with no serious violation in the ${scheme} theme`, async ({
      page,
    }) => {
      await page.emulateMedia({ colorScheme: scheme })
      await signIn(page, USERS.alice, '/admin/beta')

      await expect(page.getByRole('heading', { level: 1, name: 'Bêta' })).toBeVisible()
      for (const section of ['Activité par jour', 'Sujets', 'Le Tour', 'Fonctionnalités']) {
        await expect(page.getByRole('heading', { level: 2, name: section })).toBeVisible()
      }
      await expect(page.locator('main [aria-busy="true"]')).toHaveCount(0)
      expect(await seriousViolations(page)).toEqual([])
    })
  }

  test('refuses the report to a user who is not an admin', async ({ page }) => {
    await signIn(page, USERS.bob, '/profile')
    await page.goto('/admin/beta')

    await expect(page.getByText('Cette page est réservée aux administrateurs.')).toBeVisible()
    await expect(page.getByRole('heading', { level: 2, name: 'Sujets' })).toBeHidden()
  })
})
