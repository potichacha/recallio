import { expect, test, type Page } from '@playwright/test'
import { followSubjects, signIn, USERS } from './support/auth'

/**
 * Répond à la question affichée, juste ou non : le premier choix, ou un mot dans chaque trou. Le serveur corrige ; le
 * parcours vérifie seulement qu'il le fait.
 */
async function answerShownQuestion(page: Page, scope = page.locator('body')) {
  const radios = scope.getByRole('radio')
  const checkboxes = scope.getByRole('checkbox')
  const blanks = scope.getByRole('textbox', { name: /^Trou \d+$/ })
  if ((await radios.count()) > 0) {
    await radios.first().check()
  } else if ((await checkboxes.count()) > 0) {
    await checkboxes.first().check()
  } else {
    for (const blank of await blanks.all()) await blank.fill('x')
  }
}

/** Joue les cinq questions d'un duel, une à une, jusqu'au bout. */
async function playDuel(page: Page) {
  for (let question = 1; question <= 5; question++) {
    await expect(page.getByText(`Question ${question} sur 5`)).toBeVisible()
    await answerShownQuestion(page)
    await page.getByRole('button', { name: 'Valider' }).click()
    if (question < 5) {
      await page.getByRole('button', { name: 'Question suivante' }).click()
    }
  }
}

test.describe('Examen blanc et classé', () => {
  test('passes a mock exam in Go and gets a mark with the correction', async ({ page }) => {
    await signIn(page, USERS.alice, '/subjects/go/exam')

    await expect(page.getByRole('heading', { name: 'Examen blanc : Go' })).toBeVisible()
    await page.getByRole('button', { name: 'Commencer l’examen' }).click()
    await expect(page.getByRole('timer', { name: 'Temps restant' })).toBeVisible()
    await answerShownQuestion(page, page.getByRole('group', { name: /^Question 1 sur \d+$/ }))
    await page.getByRole('button', { name: 'Rendre ma copie' }).click()

    await expect(page.getByRole('status')).toContainText('% de bonnes réponses')
    await expect(page.getByRole('group', { name: /^Question 1 : (juste|fausse)$/ })).toBeVisible()
  })

  test('plays an asynchronous duel between two players, then both see the result', async ({
    browser,
  }) => {
    const alice = await (await browser.newContext()).newPage()
    const bob = await (await browser.newContext()).newPage()

    // Le classé montre les sujets suivis : chacun suit Go.
    await signIn(alice, USERS.alice)
    await followSubjects(alice, ['Go'])
    await alice.goto('/ranked')
    const goForAlice = alice
      .getByRole('heading', { name: 'Go', exact: true })
      .locator('xpath=ancestor::li')
    await goForAlice.getByRole('button', { name: 'Duel', exact: true }).click()
    await playDuel(alice)
    await expect(alice.getByText(/Tu as fini : \d \/ 5 bonnes réponses\./)).toBeVisible()

    await signIn(bob, USERS.bob)
    await followSubjects(bob, ['Go'])
    await bob.goto('/ranked')
    const goForBob = bob
      .getByRole('heading', { name: 'Go', exact: true })
      .locator('xpath=ancestor::li')
    await goForBob.getByRole('button', { name: 'Duel', exact: true }).click()
    await expect(
      bob.getByRole('heading', { name: 'Duel classé contre Alice Martin' }),
    ).toBeVisible()
    await playDuel(bob)

    await expect(bob.getByText(/^(Victoire !|Défaite|Match nul)$/)).toBeVisible()
    await expect(bob.getByText(/Match de placement/)).toBeVisible()
    await alice.reload()
    await expect(alice.getByText(/^(Victoire !|Défaite|Match nul)$/)).toBeVisible()
  })
})
