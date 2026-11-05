import { expect, test, type Locator } from '@playwright/test'
import { signIn, USERS } from './support/auth'

/** Une exécution passe par le vrai runner et un conteneur du bac à sable Go (ADR 0037) : quelques secondes. */
const RUN_TIMEOUT = 30_000

test.describe('Le Tour', () => {
  test('reads the Go Tour without an account', async ({ page }) => {
    await page.goto('/subjects/go/tour')
    await expect(page.getByRole('heading', { name: 'Le Tour de Go' })).toBeVisible()

    await page.getByRole('link', { name: 'Commencer le Tour' }).click()

    await expect(page.getByRole('heading', { name: 'Bienvenue' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Se connecter pour exécuter' })).toBeVisible()
  })

  test('runs the program of a page in the sandbox, then goes to the next page', async ({
    page,
  }) => {
    await signIn(page, USERS.alice, '/subjects/go')
    await page.getByRole('link', { name: 'Le Tour', exact: true }).click()
    await page.getByRole('link', { name: 'Commencer le Tour' }).click()
    await expect(page.getByRole('heading', { name: 'Bienvenue' })).toBeVisible()

    await page.getByRole('button', { name: 'Exécuter' }).click()

    await expect(page.getByLabel('Sortie standard')).toContainText(
      'Bienvenue dans le Tour de Go !',
      { timeout: RUN_TIMEOUT },
    )
    await page.getByRole('link', { name: 'Page suivante' }).click()
    await expect(page.getByRole('heading', { name: 'Le premier programme' })).toBeVisible()
  })

  test('validates the first notion with its questions, retrying with the corrections', async ({
    page,
  }) => {
    await signIn(page, USERS.alice, '/subjects/go/tour')
    await page.getByRole('link', { name: 'Commencer le Tour' }).click()
    const check = page.getByRole('region', { name: 'Valider la notion' })
    // La première notion du Tour Go n'a que des vrai ou faux et des QCM : on y répond en cochant.
    const questions = check.getByRole('group')
    await expect(questions.first()).toBeVisible()

    // Un premier essai, le premier choix partout : la correction montre ensuite les bonnes réponses. Sur une pile qui a
    // déjà servi, la notion a pu être validée avant, et elle le reste.
    for (const question of await questions.all()) {
      await question.getByRole('radio').or(question.getByRole('checkbox')).first().check()
    }
    await check.getByRole('button', { name: 'Vérifier' }).click()
    const validated = check.getByRole('status')
    const retry = check.getByRole('button', { name: 'Réessayer' })
    await expect(validated.or(retry)).toBeVisible()
    if (await retry.isVisible()) {
      const rights = await rightChoices(questions)
      await retry.click()
      await choose(questions, rights)
      await check.getByRole('button', { name: 'Vérifier' }).click()
    }

    await expect(validated).toHaveText('Notion validée !')
    await page.getByRole('navigation', { name: 'Fil d’Ariane' }).getByRole('link').click()
    await expect(page.getByRole('listitem').filter({ hasText: 'Bienvenue' })).toContainText(
      'validée',
    )
  })
})

/** Ce que la correction dit de chaque choix, question par question : fallait-il le cocher ? */
async function rightChoices(questions: Locator): Promise<boolean[][]> {
  return Promise.all(
    (await questions.all()).map((question) =>
      question
        .locator('label')
        .evaluateAll((options) =>
          options.map((option) => option.textContent?.includes('Bonne réponse') ?? false),
        ),
    ),
  )
}

/** Coche les bons choix de chaque question, et seulement eux. */
async function choose(questions: Locator, rights: boolean[][]) {
  for (const [index, question] of (await questions.all()).entries()) {
    for (const [at, input] of (await question.locator('input').all()).entries()) {
      if (rights[index]?.[at]) await input.check()
      else if ((await input.getAttribute('type')) === 'checkbox') await input.uncheck()
    }
  }
}
