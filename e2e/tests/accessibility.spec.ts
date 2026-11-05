import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'
import { followSubjects, signIn, USERS } from './support/auth'

/** Les critères WCAG 2.2, niveaux A et AA, qu'axe sait vérifier seul (ADR 0050). */
const WCAG = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

/**
 * Les violations graves ou critiques de la page, une ligne chacune : elles font échouer la CI. Les mineures ne la
 * bloquent pas ; un audit manuel (clavier, lecteur d'écran) reste nécessaire, axe ne voit pas tout.
 */
async function seriousViolations(page: Page): Promise<string[]> {
  // axe mesure les couleurs à l'instant : une transition en cours fausserait les contrastes.
  await page.waitForFunction(() =>
    document
      .getAnimations()
      .every(
        (animation) =>
          animation.playState !== 'running' ||
          animation.effect?.getComputedTiming().iterations === Infinity,
      ),
  )
  const { violations } = await new AxeBuilder({ page }).withTags(WCAG).analyze()
  return violations
    .filter(({ impact }) => impact === 'serious' || impact === 'critical')
    .map(({ id, nodes }) => `${id} : ${nodes.map((node) => node.target.join(' ')).join(', ')}`)
}

test.describe('Accessibilité', () => {
  test('the pages of a visitor have no serious violation', async ({ page }) => {
    const pages: [string, string | RegExp][] = [
      ['/', 'Prépare tes entretiens techniques, dix minutes par jour.'],
      ['/subjects', 'Apprendre'],
      ['/try/go', 'Essai : Go'],
      ['/subjects/go/tour/hello-world', /premier programme/],
    ]
    for (const [path, heading] of pages) {
      await page.goto(path)
      await expect(page.getByRole('heading', { name: heading }).first()).toBeVisible()
      expect(await seriousViolations(page), path).toEqual([])
    }
  })

  test('the pages of a signed-in user have no serious violation', async ({ page }) => {
    await signIn(page, USERS.bob, '/profile')
    await followSubjects(page, ['Java'])
    expect(await seriousViolations(page), '/profile').toEqual([])

    await page.goto('/')
    await expect(page.getByRole('heading', { name: /^Bonjour/ })).toBeVisible()
    expect(await seriousViolations(page), '/').toEqual([])

    await page.goto('/review')
    await page.getByRole('button', { name: 'Commencer' }).click()
    const card = page.locator('article').first()
    await expect(card).toBeVisible()
    expect(await seriousViolations(page), 'une carte').toEqual([])
  })
})
