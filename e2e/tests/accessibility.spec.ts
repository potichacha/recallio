import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from './support/test'
import { followSubjects, signIn, USERS } from './support/auth'

/** Les critères WCAG 2.2, niveaux A et AA, qu'axe sait vérifier seul (ADR 0050, 0058). */
const WCAG = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

/** La largeur d'un écran de 1280 px zoomé à 400 % : une page doit encore s'y lire sans défiler de côté (WCAG 1.4.10). */
const REFLOW_WIDTH = 320

/** Les pages qu'un visiteur ouvre sans compte. */
const VISITOR_PAGES = [
  '/',
  '/learn',
  '/tours',
  '/subjects',
  '/subjects/go',
  '/subjects/go/tour',
  '/subjects/go/tour/hello-world',
  '/try/go',
  '/une-adresse-qui-n-existe-pas',
]

/** Les pages d'un utilisateur connecté qui suit Java. */
const MEMBER_PAGES = [
  '/',
  '/profile',
  '/profile/subjects',
  '/profile/settings',
  '/profile/data',
  '/review',
  '/review?subject=java',
  '/subjects/java/course',
  '/subjects/java/mistakes',
  '/play',
  '/ranked',
  '/groups',
]

/** Attend que la page soit affichée : son titre est là, et plus rien ne charge. */
async function settled(page: Page) {
  await expect(page.locator('main h1').first()).toBeVisible()
  await expect(page.locator('main [aria-busy="true"]')).toHaveCount(0)
}

/**
 * Les violations graves ou critiques de la page, une ligne chacune : elles font échouer la CI. Les mineures ne la
 * bloquent pas ; ce qu'axe ne voit pas (l'ordre de lecture, le sens des libellés) se vérifie à la main, avec
 * docs/accessibilite.md.
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

/** Répond à la carte affichée sans chercher la bonne réponse, pour en voir la correction. */
async function answerBlindly(page: Page) {
  const card = page.locator('main form').first()
  const choice = card.getByRole('radio').or(card.getByRole('checkbox')).first()
  await expect(choice.or(card.getByRole('textbox').first())).toBeVisible()
  if (await choice.isVisible()) {
    await choice.check()
  } else {
    for (const blank of await card.getByRole('textbox').all()) await blank.fill('x')
  }
  await card.getByRole('button', { name: 'Vérifier' }).click()
  await expect(card.getByText(/^(Juste !|Pas tout à fait\.)$/)).toBeVisible()
}

// Chaque thème a ses couleurs (ADR 0058) : axe remesure chaque contraste dans l'un et dans l'autre.
for (const scheme of ['light', 'dark'] as const) {
  test.describe(`Accessibilité, thème ${scheme === 'light' ? 'clair' : 'sombre'}`, () => {
    test.use({ colorScheme: scheme })

    test('the pages of a visitor have no serious violation', async ({ page }) => {
      for (const path of VISITOR_PAGES) {
        await page.goto(path)
        await settled(page)
        await expect(page.locator('html')).toHaveAttribute('data-theme', scheme)
        expect(await seriousViolations(page), path).toEqual([])
      }
      // Une carte corrigée : le verdict, ses icônes et ses couleurs.
      await page.goto('/try/go')
      await answerBlindly(page)
      expect(await seriousViolations(page), 'une carte corrigée').toEqual([])
    })

    test('the pages of a signed-in user have no serious violation', async ({ page }) => {
      await signIn(page, USERS.bob, '/profile')
      await followSubjects(page, ['Java'])
      for (const path of MEMBER_PAGES) {
        await page.goto(path)
        await settled(page)
        expect(await seriousViolations(page), path).toEqual([])
      }
      // Une carte de révision, avant d'y répondre.
      await page.goto('/review?subject=java')
      await page.getByRole('button', { name: 'Commencer' }).click()
      await expect(page.locator('article').first()).toBeVisible()
      expect(await seriousViolations(page), 'une carte').toEqual([])
    })
  })
}

test.describe('Accessibilité', () => {
  // Reflow (WCAG 1.4.10) : une page ne défile que de haut en bas, sur un téléphone comme sur un écran zoomé à 400 %.
  test('no page scrolls sideways at 320 px wide @mobile', async ({ page }) => {
    await page.setViewportSize({ width: REFLOW_WIDTH, height: 640 })
    for (const path of VISITOR_PAGES) {
      await page.goto(path)
      await settled(page)
      const overflow = await page.evaluate(
        () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
      )
      expect(overflow, path).toBe(0)
    }
  })

  // Sauter la navigation (WCAG 2.4.1) : le premier arrêt du clavier mène au contenu.
  test('the first stop of the keyboard skips the navigation', async ({ page }) => {
    await page.goto('/learn')
    await settled(page)

    await page.keyboard.press('Tab')
    const skip = page.getByRole('link', { name: 'Aller au contenu' })
    await expect(skip).toBeFocused()
    await expect(skip).toBeVisible()
    await page.keyboard.press('Enter')

    await expect(page.locator('main')).toBeFocused()
  })

  // Un titre par page (WCAG 2.4.2) : l'onglet dit où l'on est, et un lecteur d'écran l'annonce.
  test('names the tab after the page', async ({ page }) => {
    await page.goto('/learn')
    await expect(page).toHaveTitle('Cours · Recallio')

    await page.getByRole('link', { name: 'Tour', exact: true }).click()
    await expect(page).toHaveTitle('Le Tour · Recallio')
  })
})
