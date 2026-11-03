import { expect, test } from '@playwright/test'
import { signIn, USERS } from './support/auth'

test.describe('Parcours', () => {
  test('opens the path of Java from its page and sees its notions with their status', async ({
    page,
  }) => {
    await signIn(page, USERS.bob, '/subjects')
    await page
      .getByRole('region', { name: 'Langages' })
      .getByRole('link', { name: /^Java/ })
      .click()

    await page.getByRole('link', { name: 'Mon parcours' }).click()

    await expect(page.getByRole('heading', { name: 'Mon parcours Java' })).toBeVisible()
    const firstSteps = page.getByRole('region', { name: 'Premiers pas' })
    await expect(firstSteps.getByRole('listitem').first()).toContainText(
      /À venir|Verrouillée|Disponible|En cours|Acquise/,
    )
    await expect(page.getByText(/\d+ \/ \d+ notions acquises/)).toBeVisible()
  })
})
