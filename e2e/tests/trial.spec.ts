import { expect, test } from '@playwright/test'

test.describe('Essai sans compte', () => {
  test('answers the questions of the Go trial, then is invited to create an account', async ({
    page,
  }) => {
    await page.goto('/')
    await page
      .getByRole('region', { name: 'Essaie sans compte' })
      .getByRole('link', { name: 'Essayer Go' })
      .click()
    await expect(page.getByRole('heading', { name: 'Essai : Go' })).toBeVisible()

    const counter = page.getByText(/^Question \d+ sur \d+$/)
    const total = Number((await counter.textContent())?.split(' ').at(-1))
    expect(total).toBeGreaterThan(0)
    const form = page.locator('main form')
    for (let question = 1; question <= total; question++) {
      await expect(counter).toHaveText(`Question ${question} sur ${total}`)
      // Des formats corrigés par le serveur : on coche le premier choix, ou on remplit les trous.
      const choice = form.getByRole('radio').or(form.getByRole('checkbox')).first()
      if (await choice.isVisible()) {
        await choice.check()
      } else {
        for (const blank of await form.getByRole('textbox').all()) await blank.fill('x')
      }
      await form.getByRole('button', { name: 'Vérifier' }).click()
      await expect(form.getByText(/^(Juste !|Pas tout à fait\.)$/)).toBeVisible()
      await form.getByRole('button', { name: 'Question suivante' }).click()
    }

    const summary = page.getByRole('region', { name: 'Bilan de l’essai' })
    await expect(summary).toContainText(`/ ${total}`)
    await expect(summary.getByRole('button', { name: 'Créer mon compte' })).toBeVisible()
  })
})
