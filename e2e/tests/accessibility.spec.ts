import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from './support/test'
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

/** Les pages qu'un visiteur ouvre sans compte, et le titre qui dit qu'elles sont affichées. */
const VISITOR_PAGES: [string, string | RegExp][] = [
  ['/', 'Prêt pour ton entretien technique, dix minutes par jour.'],
  ['/learn', 'Cours'],
  ['/tours', 'Le Tour'],
  ['/subjects', 'Toutes les cartes'],
  ['/try/go', 'Essai : Go'],
  ['/subjects/go/tour/hello-world', /premier programme/],
]

test.describe('Accessibilité', () => {
  test('the pages of a visitor have no serious violation', async ({ page }) => {
    for (const [path, heading] of VISITOR_PAGES) {
      await page.goto(path)
      await expect(page.getByRole('heading', { name: heading }).first()).toBeVisible()
      expect(await seriousViolations(page), path).toEqual([])
    }
  })

  // Le thème sombre (ADR 0058) a ses propres couleurs : axe y remesure chaque contraste.
  test.describe('dans le thème sombre', () => {
    test.use({ colorScheme: 'dark' })

    test('the pages of a visitor have no serious violation in the dark theme', async ({ page }) => {
      for (const [path, heading] of VISITOR_PAGES) {
        await page.goto(path)
        await expect(page.getByRole('heading', { name: heading }).first()).toBeVisible()
        await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
        expect(await seriousViolations(page), path).toEqual([])
      }
    })
  })

  // Reflow (WCAG 1.4.10) : sur un téléphone, une page ne défile que de haut en bas. Un long titre de sujet faisait
  // déborder la grille de quelques pixels.
  test('the subject pages do not scroll sideways on a phone @mobile', async ({ page }) => {
    const pages: [string, string][] = [
      ['/', 'Prêt pour ton entretien technique, dix minutes par jour.'],
      ['/learn', 'Cours'],
      ['/tours', 'Le Tour'],
    ]
    for (const [path, heading] of pages) {
      await page.goto(path)
      await expect(page.getByRole('heading', { name: heading }).first()).toBeVisible()
      await expect(page.getByRole('heading', { name: 'Fondamentaux' })).toBeVisible()
      const overflow = await page.evaluate(
        () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
      )
      expect(overflow, path).toBe(0)
    }
  })

  test('the pages of a signed-in user have no serious violation', async ({ page }) => {
    await signIn(page, USERS.bob, '/profile')
    await followSubjects(page, ['Java'])
    expect(await seriousViolations(page), '/profile/subjects').toEqual([])
    // Les autres sections du profil, chacune à son adresse.
    const sections: [string, string][] = [
      ['/profile', 'Nom affiché'],
      ['/profile/settings', 'Sons et vibrations'],
      ['/profile/data', 'Mes données'],
    ]
    for (const [path, text] of sections) {
      await page.goto(path)
      await expect(page.getByText(text).first()).toBeVisible()
      expect(await seriousViolations(page), path).toEqual([])
    }

    await page.goto('/')
    await expect(page.getByRole('heading', { name: /^(Salut|Bienvenue)/ })).toBeVisible()
    expect(await seriousViolations(page), '/').toEqual([])

    await page.goto('/review')
    expect(await seriousViolations(page), '/review').toEqual([])
    await page.getByRole('region', { name: 'Langages' }).getByRole('link', { name: 'Java' }).click()
    await page.getByRole('button', { name: 'Commencer' }).click()
    const card = page.locator('article').first()
    await expect(card).toBeVisible()
    expect(await seriousViolations(page), 'une carte').toEqual([])
  })
})
