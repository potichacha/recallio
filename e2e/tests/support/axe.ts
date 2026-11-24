import AxeBuilder from '@axe-core/playwright'
import type { Page } from '@playwright/test'

/** Les critères WCAG 2.2, niveaux A et AA, qu'axe sait vérifier seul (ADR 0050, 0058). */
export const WCAG = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

/**
 * Les violations graves ou critiques de la page, une ligne chacune : elles font échouer la CI. Les mineures ne la
 * bloquent pas ; ce qu'axe ne voit pas (l'ordre de lecture, le sens des libellés) se vérifie à la main, avec
 * docs/accessibilite.md.
 */
export async function seriousViolations(page: Page): Promise<string[]> {
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
  // axe mesure une cible là où la page est défilée : un lien à moitié glissé sous l'en-tête collant lui paraît trop
  // petit (« target-size »). En haut de page, rien n'est sous l'en-tête, et le résultat ne dépend plus du défilement.
  await page.evaluate(() => window.scrollTo(0, 0))
  const { violations } = await new AxeBuilder({ page }).withTags(WCAG).analyze()
  return violations
    .filter(({ impact }) => impact === 'serious' || impact === 'critical')
    .map(({ id, nodes }) => `${id} : ${nodes.map((node) => node.target.join(' ')).join(', ')}`)
}
