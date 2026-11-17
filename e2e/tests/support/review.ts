import { expect, type Page } from '@playwright/test'

/**
 * Répond à la carte de révision affichée, sans chercher la bonne réponse : toute carte se corrige sur le serveur
 * (ADR 0051). Une carte à choix se valide sur son premier choix affiché, un code à trous avec « x » dans chaque trou.
 */
export async function answerCard(page: Page) {
  const card = page.locator('article').first()
  const validate = card.getByRole('button', { name: 'Valider' })
  await expect(validate).toBeVisible()
  const choice = card.getByRole('radio').or(card.getByRole('checkbox')).first()
  if (await choice.isVisible()) {
    await choice.check()
  } else {
    for (const blank of await card.getByRole('textbox').all()) await blank.fill('x')
  }
  await validate.click()
  await card.getByRole('button', { name: 'Carte suivante' }).click()
}
