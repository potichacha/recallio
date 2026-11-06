import { expect, type Page } from '@playwright/test'

/**
 * Répond à la carte de révision affichée, quelle qu'elle soit. Une carte à choix (ADR 0051) se valide sur son premier
 * choix affiché, et le serveur la corrige ; une carte qui n'a pas encore de choix se retourne, puis « Je savais ».
 */
export async function answerCard(page: Page) {
  const card = page.locator('article').first()
  const validate = card.getByRole('button', { name: 'Valider' })
  const flip = card.getByRole('button', { name: 'Voir la réponse' })
  await expect(validate.or(flip)).toBeVisible()
  if (await validate.isVisible()) {
    await card.getByRole('radio').or(card.getByRole('checkbox')).first().check()
    await validate.click()
    await card.getByRole('button', { name: 'Carte suivante' }).click()
    return
  }
  await flip.click()
  await page
    .getByRole('group', { name: 'Connaissais-tu la réponse ?' })
    .getByRole('button', { name: 'Je savais' })
    .click()
}
