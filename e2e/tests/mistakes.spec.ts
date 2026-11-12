import { signIn, USERS } from './support/auth'
import { expect, test, type Locator } from './support/test'

/** Assez de cartes pour en rater une : chacune est validée sur son premier choix, ou sur un trou rempli de travers. */
const MAX_CARDS = 12

/** Répond à la carte affichée sans chercher la bonne réponse : le premier choix, ou « x » dans chaque trou. */
async function answerBlindly(card: Locator) {
  const choice = card.getByRole('radio').or(card.getByRole('checkbox')).first()
  if (await choice.isVisible()) {
    await choice.check()
  } else {
    for (const blank of await card.getByRole('textbox').all()) await blank.fill('x')
  }
}

test.describe('Apprendre de ses erreurs (ADR 0057)', () => {
  test('fails a card, reads its lesson again, then finds it in the mistakes of the week', async ({
    page,
  }) => {
    await signIn(page, USERS.bob, '/review?subject=go')
    // Le bouton radio est masqué derrière son libellé : c'est lui qu'on touche.
    await page.getByText('Questions corrigées', { exact: true }).click()
    await page.getByRole('button', { name: 'Commencer' }).click()

    const card = page.locator('article').first()
    let failed = false
    for (let reviewed = 0; reviewed < MAX_CARDS && !failed; reviewed++) {
      const validate = card.getByRole('button', { name: 'Valider' })
      await expect(validate).toBeVisible()
      await answerBlindly(card)
      await validate.click()
      const verdict = card.getByRole('status')
      await expect(verdict).toBeVisible()
      failed = /Pas tout à fait/.test((await verdict.textContent()) ?? '')
      if (!failed) await card.getByRole('button', { name: 'Carte suivante' }).click()
    }
    expect(failed, `aucune carte ratée en ${MAX_CARDS} réponses à l'aveugle`).toBe(true)

    // Sous une réponse fausse, la leçon de la notion est à un clic, dans un autre onglet.
    const lesson = card.getByRole('link', { name: /Revoir la leçon/ })
    await expect(lesson).toHaveAttribute('href', /^\/subjects\/go\/course\//)
    await expect(lesson).toHaveAttribute('target', '_blank')

    await page.goto('/review?subject=go')
    await page.getByRole('link', { name: /Revoir mes erreurs de la semaine \(\d+\)/ }).click()

    await expect(
      page.getByRole('heading', { name: 'Mes erreurs de la semaine : Go' }),
    ).toBeVisible()
    await expect(page.getByText(/^Carte 1 sur \d+/)).toBeVisible()
    const mistake = page.locator('main form')
    await answerBlindly(mistake)
    await mistake.getByRole('button', { name: 'Vérifier' }).click()
    await expect(mistake.getByText(/^(Juste !|Pas tout à fait\.)$/)).toBeVisible()
    await expect(mistake.getByRole('button', { name: 'Carte suivante' })).toBeVisible()
  })
})
