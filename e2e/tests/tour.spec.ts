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

  test('runs the program of a page in the sandbox, goes to the next page, then resumes there', async ({
    page,
  }) => {
    await signIn(page, USERS.alice, '/subjects/go')
    await page.getByRole('link', { name: 'Le Tour', exact: true }).click()
    // Le lien de la première notion : « Commencer le Tour » devient « Reprendre le Tour » dès la première page ouverte.
    await page.getByRole('link', { name: 'Bienvenue', exact: true }).click()
    await expect(page.getByRole('heading', { name: 'Bienvenue' })).toBeVisible()

    await page.getByRole('button', { name: 'Exécuter' }).click()

    await expect(page.getByLabel('Sortie standard')).toContainText(
      'Bienvenue dans le Tour de Go !',
      { timeout: RUN_TIMEOUT },
    )
    await page.getByRole('link', { name: 'Page suivante' }).click()
    await expect(page.getByRole('heading', { name: 'Le premier programme' })).toBeVisible()

    await page.getByRole('navigation', { name: 'Fil d’Ariane' }).getByRole('link').click()
    await expect(page.getByRole('link', { name: 'Reprendre le Tour' })).toHaveAttribute(
      'href',
      '/subjects/go/tour/hello-world',
    )
  })

  test('validates the first notion with its questions, retrying with the corrections', async ({
    page,
  }) => {
    await signIn(page, USERS.alice, '/subjects/go/tour')
    await page.getByRole('link', { name: 'Bienvenue', exact: true }).click()
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

  test('practices every card of a notion, the server correcting what it can', async ({ page }) => {
    await signIn(page, USERS.alice, '/subjects/go/tour')
    await page.getByRole('link', { name: 'Bienvenue', exact: true }).click()
    await page.getByRole('link', { name: 'S’entraîner sur cette notion' }).click()

    await expect(page.getByRole('heading', { name: /^S’entraîner : / })).toBeVisible()
    await expect(page.getByText(/^Carte 1 sur \d+$/)).toBeVisible()
    // Les formats alternent : une question de cours, qu'on retourne, puis un vrai ou faux, corrigé par le serveur.
    await page.getByRole('button', { name: 'Voir la réponse' }).click()
    await expect(page.getByRole('region', { name: 'Réponse' })).toBeVisible()
    await page.getByRole('button', { name: 'Carte suivante' }).click()

    await expect(page.getByText(/^Carte 2 sur \d+$/)).toBeVisible()
    await page.getByRole('radio').first().check()
    await page.getByRole('button', { name: 'Vérifier' }).click()
    await expect(page.getByText(/^(Juste !|Pas tout à fait\.)$/)).toBeVisible()
    await page.getByRole('link', { name: 'Revenir au Tour' }).click()
    await expect(page.getByRole('heading', { name: 'Bienvenue' })).toBeVisible()
  })

  test('checks the program of an « À toi » in the sandbox, without revealing the output', async ({
    page,
  }) => {
    await signIn(page, USERS.alice, '/subjects/go/tour')
    await page.getByRole('link', { name: 'Le premier programme', exact: true }).click()
    const task = page.getByRole('region', { name: 'À toi' })

    // Le code de départ, tel quel, ne donne pas la sortie attendue.
    await task.getByRole('button', { name: 'Vérifier mon programme' }).click()

    await expect(
      task.getByText('Pas encore : la sortie ne correspond pas à celle attendue.'),
    ).toBeVisible({ timeout: RUN_TIMEOUT })
    await expect(task.getByLabel('Sortie standard')).toContainText('Go a été rendu public en ...')
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
