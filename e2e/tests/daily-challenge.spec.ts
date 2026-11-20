import { expect, test, type Locator } from './support/test'
import { signIn, USERS } from './support/auth'
import { seriousViolations } from './support/axe'

/** Répond à la question affichée, juste ou non : le premier choix, ou « x » dans chaque trou. Le serveur corrige. */
async function answerShownQuestion(form: Locator) {
  const choice = form.getByRole('radio').or(form.getByRole('checkbox')).first()
  if (await choice.isVisible()) {
    await choice.check()
  } else {
    for (const blank of await form.getByRole('textbox').all()) await blank.fill('x')
  }
}

test.describe('Défi du jour (ADR 0067)', () => {
  test('plays the Go challenge of the day to the end, sees the score, the figures and the share, and finds the result again', async ({
    page,
  }) => {
    await signIn(page, USERS.alice, '/daily/go')
    await expect(page.getByRole('heading', { name: 'Défi du jour : Go' })).toBeVisible()

    const result = page.getByRole('region', { name: /^(Défi terminé|Sans faute !)$/ })
    const counter = page.getByText(/^Question \d+ sur \d+$/)
    // Une base déjà utilisée peut garder le défi fini par un passage précédent : on ne joue que s'il en reste.
    await expect(result.or(counter)).toBeVisible()
    if (await counter.isVisible()) {
      const [, first, total] =
        /^Question (\d+) sur (\d+)$/.exec((await counter.textContent()) ?? '') ?? []
      const form = page.locator('main form')
      for (let question = Number(first); question <= Number(total); question++) {
        await expect(counter).toHaveText(`Question ${question} sur ${total}`)
        await answerShownQuestion(form)
        await form.getByRole('button', { name: 'Valider' }).click()
        await expect(form.getByText(/^(Juste !|Pas tout à fait\.)$/)).toBeVisible()
        const last = question === Number(total)
        await form
          .getByRole('button', { name: last ? 'Voir mon résultat' : 'Question suivante' })
          .click()
      }
    }

    await expect(result).toBeVisible()
    // Le score est le seul paragraphe « 4/5 » : la répartition des scores écrit aussi « 5/5 » à « 0/5 », dans des span.
    await expect(result.locator('p').filter({ hasText: /^\d\/5$/ })).toBeVisible()
    await expect(
      result.getByRole('list', { name: 'Tes réponses' }).getByRole('listitem'),
    ).toHaveCount(5)
    const figures = result.getByRole('region', { name: 'Les chiffres du jour' })
    await expect(figures.getByText(/^\d+ joueurs? (a|ont) fini ce défi/)).toBeVisible()
    await expect(figures.getByRole('list', { name: 'Répartition des scores' })).toBeVisible()
    await expect(result.getByRole('button', { name: 'Partager', exact: true })).toBeVisible()
    await expect(
      result.getByText('Reviens demain pour le prochain défi.', { exact: true }),
    ).toBeVisible()
    expect(await seriousViolations(page), 'le résultat du défi du jour').toEqual([])

    // Revenir sur la page montre le résultat, jamais un second essai.
    await page.reload()
    await expect(result).toBeVisible()
    await expect(page.getByRole('button', { name: 'Valider' })).toHaveCount(0)
  })
})
