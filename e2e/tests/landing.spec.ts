import { expect, test } from './support/test'

test.describe('Page publique (ADR 0058)', () => {
  test('answers a real question from the home page, then goes on with the trial @mobile', async ({
    page,
  }) => {
    await page.goto('/')
    await expect(
      page.getByRole('heading', {
        name: 'Prêt pour ton entretien technique, dix minutes par jour.',
      }),
    ).toBeVisible()

    // La première question de l'essai est posée dans la page : on y répond sans rien remplir d'autre.
    const question = page.getByRole('region', { name: 'Essaie sans compte' }).locator('form')
    const choice = question.getByRole('radio').or(question.getByRole('checkbox')).first()
    await expect(choice.or(question.getByRole('textbox').first())).toBeVisible()
    if (await choice.isVisible()) {
      await choice.check()
    } else {
      for (const blank of await question.getByRole('textbox').all()) await blank.fill('x')
    }
    await question.getByRole('button', { name: 'Vérifier' }).click()
    await expect(question.getByText(/^(Juste !|Pas tout à fait\.)$/)).toBeVisible()

    await question.getByRole('link', { name: /^Continuer : encore \d+ questions?$/ }).click()
    await expect(page.getByRole('heading', { name: /^Essai : / })).toBeVisible()
    await expect(page.getByText(/^Question 2 sur \d+$/)).toBeVisible()

    // Les chiffres viennent du catalogue : des milliers de cartes, pas un texte écrit à la main.
    await page.goto('/')
    const figures = page.getByRole('list', { name: 'Le catalogue en chiffres' })
    await expect(figures.getByText('cartes')).toBeVisible()
    await expect(figures.getByText('exercices de code')).toBeVisible()
    await expect(page.getByRole('region', { name: 'Pour qui' })).toBeVisible()
    await expect(page.getByRole('region', { name: 'Comment ça marche' })).toBeVisible()
  })
})
