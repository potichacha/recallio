import { expect, test } from '@playwright/test'
import { signIn, USERS } from './support/auth'

test.describe('Parcours', () => {
  test('opens the skill map of Java from its page and sees its notions with their mastery', async ({
    page,
  }) => {
    await signIn(page, USERS.bob, '/subjects')
    await page
      .getByRole('region', { name: 'Langages' })
      .getByRole('link', { name: /^Java/ })
      .click()

    await page.getByRole('link', { name: 'Mon parcours' }).click()

    await expect(page.getByRole('heading', { name: 'Mon parcours Java' })).toBeVisible()
    // Le domaine où l'on en est se déplie d'emblée : « Bases du langage », qui a encore des notions à acquérir.
    const firstSteps = page.getByRole('region', { name: 'Premiers pas' })
    await expect(firstSteps.getByRole('listitem').first()).toContainText(
      /À venir|Verrouillée|Nouvelle|Découverte|Acquise|Maîtrisée|Experte/,
    )
    await expect(
      page.getByRole('img', { name: /^Java : \d+ \/ \d+ notions acquises/ }),
    ).toBeVisible()
  })
})
