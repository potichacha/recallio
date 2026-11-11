import { expect, test } from './support/test'
import { followSubjects, signIn, USERS } from './support/auth'

test.describe('Session de révision', () => {
  // Go n'a pas encore reçu ses choix (ADR 0051) : ses premières cartes s'auto-évaluent encore.
  test('reviews one subject for a chosen length, flipping self-assessed cards @mobile', async ({
    page,
  }) => {
    await signIn(page, USERS.bob, '/profile')
    await followSubjects(page, ['Java', 'Go'])
    await page.goto('/review')

    // Un sujet à la fois, choisi par son logo (ADR 0052), pour une durée choisie (ADR 0048).
    await page.getByRole('region', { name: 'Langages' }).getByRole('link', { name: 'Go' }).click()
    await expect(page.getByRole('heading', { name: 'Réviser Go' })).toBeVisible()
    await page.getByRole('group', { name: 'Durée' }).getByText('5 min', { exact: true }).click()
    await page.getByRole('button', { name: 'Commencer' }).click()
    await expect(page.getByRole('heading', { name: 'Réviser : Go' })).toBeVisible()
    await expect(page.getByRole('timer', { name: 'Temps restant' })).toHaveText('Encore 5 min')

    const answers = page.getByRole('group', { name: 'Connaissais-tu la réponse ?' })
    for (const [index, answer] of ['Je savais', 'Je ne savais pas'].entries()) {
      await expect(page.locator('article header')).toContainText('Go ·')
      await page.getByRole('button', { name: 'Voir la réponse' }).click()
      await answers.getByRole('button', { name: answer }).click()
      await expect(
        page.getByText(`${index + 1} carte${index > 0 ? 's' : ''} révisée`),
      ).toBeVisible()
    }

    await page.getByRole('button', { name: 'Terminer la session' }).click()
    await expect(page.getByRole('region', { name: 'Bilan de la session' })).toContainText(
      '2 cartes révisées, dont 1 réussie.',
    )

    await page.getByRole('link', { name: 'Recallio' }).click()
    const subjects = page.getByRole('region', { name: 'Progression par sujet' })
    await expect(subjects.getByRole('listitem').first()).toContainText(/\d+ \/ \d+ vues/)
    await expect(page.getByLabel(/Série : \d+ jours?/)).toBeVisible()
  })

  test('reviews a single theme from its page, answering by choosing (ADR 0051)', async ({
    page,
  }) => {
    await signIn(page, USERS.bob, '/subjects')
    await page
      .getByRole('region', { name: 'Langages' })
      .getByRole('link', { name: /^Java/ })
      .click()
    await page.getByRole('link', { name: /Bases du langage/ }).click()

    await page.getByRole('link', { name: 'Réviser ce thème' }).click()

    await expect(page).toHaveURL(/\/review\?theme=/)
    // L'URL change avant l'affichage de la page : on attend la page de révision elle-même.
    await expect(page.getByRole('heading', { name: 'Réviser', exact: true })).toBeVisible()
    const card = page.locator('article').first()
    // La carte dit son sujet et sa notion ; le thème, lui, est dans l'URL.
    await expect(card.locator('header')).toContainText('Java · ')
    // Les cartes de « Premiers pas » ont leurs choix : plus de carte à retourner ni d'auto-évaluation.
    await expect(card.getByRole('button', { name: 'Voir la réponse' })).toHaveCount(0)
    await card.getByRole('radio').or(card.getByRole('checkbox')).first().check()
    await card.getByRole('button', { name: 'Valider' }).click()
    await expect(card.getByRole('status')).toHaveText(/Bonne réponse|Pas tout à fait/)
    await expect(card.getByRole('button', { name: 'Carte suivante' })).toBeVisible()
  })
})
