import { expect, test, type Page } from './support/test'

/** Les animations de la page qui dureraient assez pour se voir : aucune, quand on a demandé moins d'animations. */
function visibleAnimations(page: Page) {
  return page.evaluate(() =>
    document
      .getAnimations()
      .map((animation) => Number(animation.effect?.getComputedTiming().duration ?? 0))
      .filter((duration) => duration > 1),
  )
}

test.describe('Mouvement (ADR 0058)', () => {
  test.use({ reducedMotion: 'reduce' })

  test('stops every animation for who asked for less motion, on the home and on an answered card', async ({
    page,
  }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    expect(await visibleAnimations(page), 'accueil').toEqual([])

    // Une carte de l'essai, puis son verdict : Rico ne sautille pas, le verdict ne saute pas.
    await page.goto('/try/go')
    const card = page.locator('main form')
    const choice = card.getByRole('radio').or(card.getByRole('checkbox')).first()
    await expect(choice.or(card.getByRole('textbox').first())).toBeVisible()
    if (await choice.isVisible()) {
      await choice.check()
    } else {
      for (const blank of await card.getByRole('textbox').all()) await blank.fill('x')
    }
    await card.getByRole('button', { name: 'Vérifier' }).click()
    await expect(card.getByText(/^(Juste !|Pas tout à fait\.)$/)).toBeVisible()
    expect(await visibleAnimations(page), 'carte corrigée').toEqual([])
  })
})
