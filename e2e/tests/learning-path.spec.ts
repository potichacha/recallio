import { expect, test } from './support/test'
import { signIn, USERS } from './support/auth'

test.describe('Cours', () => {
  test('opens the course of Java from the door « Apprendre », with the mastery of its notions, then reads a lesson', async ({
    page,
  }) => {
    await signIn(page, USERS.bob, '/learn')
    // Chaque sujet de la porte mène à son Tour, à son cours et à ses cartes (ADR 0077).
    await page
      .getByRole('region', { name: 'Langages' })
      .getByRole('link', { name: 'Le cours de Java' })
      .click()

    await expect(page.getByRole('heading', { name: 'Le cours de Java' })).toBeVisible()
    // Le domaine où l'on en est se déplie d'emblée : « Bases du langage », qui a encore des notions à acquérir.
    const firstSteps = page.getByRole('region', { name: 'Premiers pas' })
    await expect(firstSteps.getByRole('listitem').first()).toContainText(
      /À venir|Verrouillée|Nouvelle|Découverte|Acquise|Maîtrisée|Experte/,
    )
    await expect(
      page.getByRole('img', { name: /^Java : \d+ \/ \d+ notions acquises/ }),
    ).toBeVisible()

    // Le titre d'une notion ouvre sa leçon (ADR 0052).
    await firstSteps.getByRole('listitem').first().getByRole('link').first().click()
    await expect(page.getByRole('link', { name: 'S’entraîner sur cette notion' })).toBeVisible()
    await expect(page.getByRole('navigation', { name: 'Leçons voisines' })).toBeVisible()
  })
})
