import { expect, test, type Locator } from './support/test'
import { signIn, USERS } from './support/auth'
import { seriousViolations } from './support/axe'

/** Au plus les douze domaines des bases de Go : une borne contre une boucle sans fin si la page cassait. */
const MAX_DOMAINS = 20

/** Répond à chaque question du domaine, juste ou non : le premier choix, ou « x » dans chaque trou. Le serveur corrige. */
async function answerDomain(domain: Locator) {
  for (const question of await domain.getByRole('group').all()) {
    const choice = question.getByRole('radio').or(question.getByRole('checkbox')).first()
    if (await choice.isVisible()) {
      await choice.check()
    } else {
      for (const blank of await question.getByRole('textbox').all()) await blank.fill('x')
    }
  }
}

test.describe('Test de niveau (ADR 0068)', () => {
  test('takes the Go level check to the end, sees which domains are skipped, then cancels it', async ({
    page,
  }) => {
    await signIn(page, USERS.alice, '/subjects/go/level-check')
    await expect(page.getByRole('heading', { name: 'Test de niveau : Go' })).toBeVisible()
    // Une base déjà utilisée peut garder un test précédent : l'introduction le dit, et le test se repasse.
    await page.getByRole('button', { name: 'Commencer le test', exact: true }).click()

    const result = page.getByRole('region', { name: 'Test terminé', exact: true })
    const domain = page.getByRole('region', { name: /^Domaine \d+ sur \d+ : / })
    // « Valider » n'est que dans un domaine pas encore corrigé : le domaine suivant est bien là.
    const validate = domain.getByRole('button', { name: 'Valider', exact: true })
    // Les réponses sont justes ou non : le test s'arrête après deux domaines ratés de suite, ou au dernier.
    for (let asked = 0; asked < MAX_DOMAINS; asked++) {
      await expect(result.or(validate)).toBeVisible()
      if (await result.isVisible()) break
      await answerDomain(domain)
      await validate.click()
      await expect(
        domain.getByText(/^(Domaine réussi : tu sautes \d+ notions?|À voir dans le Tour)$/),
      ).toBeVisible()
      await domain.getByRole('button', { name: /^(Domaine suivant|Voir le résultat)$/ }).click()
    }

    await expect(result).toBeVisible()
    await expect(
      result.getByRole('list', { name: 'Les domaines des bases' }).getByRole('listitem').first(),
    ).toBeVisible()
    await expect(result.getByRole('link', { name: 'Continuer le Tour', exact: true })).toBeVisible()
    expect(await seriousViolations(page), 'le résultat du test de niveau').toEqual([])

    await result.getByRole('button', { name: 'Annuler le test', exact: true }).click()
    await page.getByRole('button', { name: 'Oui, annuler le test', exact: true }).click()
    await expect(
      page.getByText('Test annulé : le Tour et tes révisions reprennent toutes les bases.', {
        exact: true,
      }),
    ).toBeVisible()
    await expect(page.getByRole('button', { name: 'Commencer le test', exact: true })).toBeVisible()
    await expect(page.getByText(/^Tu as déjà passé ce test/)).toHaveCount(0)
  })
})
