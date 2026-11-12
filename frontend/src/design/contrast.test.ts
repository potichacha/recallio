/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

// Lue telle quelle sur le disque : importée, la feuille de style passerait par Tailwind.
const stylesheet = readFileSync('src/index.css', 'utf8')

/**
 * Les contrastes des tokens (ADR 0058), calculés selon WCAG 2.2 dans les deux thèmes : un texte a 4,5 de contraste avec
 * son fond, le contour d'un contrôle ou un pictogramme 3. Changer une couleur de index.css sans revérifier ses paires
 * fait échouer ce test.
 */
const TEXT = 4.5
const GRAPHIC = 3

const BACKGROUNDS = ['canvas', 'surface', 'surface-muted']
const STATES = ['success', 'danger', 'warning', 'info']
const TIERS = ['bronze', 'silver', 'gold', 'platinum', 'diamond', 'master'].map(
  (tier) => `tier-${tier}`,
)

/** [couleur, fond, seuil] */
const PAIRS: [string, string, number][] = [
  ...['ink', 'ink-soft', 'ink-muted', 'ink-faint'].flatMap((ink) =>
    [...BACKGROUNDS, 'surface-sunken'].map((background): [string, string, number] => [
      ink,
      background,
      TEXT,
    ]),
  ),
  ...['brand', 'brand-strong'].flatMap((brand) =>
    [...BACKGROUNDS, 'brand-soft'].map((background): [string, string, number] => [
      brand,
      background,
      TEXT,
    ]),
  ),
  ['on-brand', 'brand', TEXT],
  ['on-brand', 'brand-strong', TEXT],
  ['on-sun', 'sun', TEXT],
  ['on-success', 'success', TEXT],
  ['on-success', 'success-strong', TEXT],
  ...STATES.flatMap((state) =>
    [...BACKGROUNDS, `${state}-soft`].map((background): [string, string, number] => [
      state,
      background,
      TEXT,
    ]),
  ),
  ['success-strong', 'success-soft', TEXT],
  ['danger-strong', 'danger-soft', TEXT],
  ['danger-strong', 'surface', TEXT],
  ...['danger-soft', 'success-soft', 'sun-soft', 'brand-soft', 'warning-soft', 'info-soft'].map(
    (background): [string, string, number] => ['ink', background, TEXT],
  ),
  ['ink-muted', 'brand-soft', TEXT],
  ...BACKGROUNDS.map((background): [string, string, number] => [
    'line-strong',
    background,
    GRAPHIC,
  ]),
  // Les contours d'un choix : sélectionné, juste, faux (ADR 0058).
  ...['brand', 'success', 'danger'].map((border): [string, string, number] => [
    border,
    'surface',
    GRAPHIC,
  ]),
  // Le focus posé sur un fond à la couleur de la marque.
  ['on-brand', 'brand', GRAPHIC],
  ['flame', 'surface', GRAPHIC],
  ['flame', 'canvas', GRAPHIC],
  ...TIERS.flatMap((tier) =>
    ['surface', 'canvas'].map((background): [string, string, number] => [
      tier,
      background,
      GRAPHIC,
    ]),
  ),
  ...['ink', 'muted', 'keyword', 'string', 'number', 'title', 'comment', 'error'].map(
    (role): [string, string, number] => [`code-${role}`, 'code', TEXT],
  ),
]

/** Les tokens `--color-nom: #rrggbb;` d'un bloc de la feuille de style. */
function tokens(block: string): Record<string, string> {
  return Object.fromEntries(
    [...block.matchAll(/--color-([a-z-]+):\s*(#[0-9a-f]{6});/g)].map(([, name, value]) => [
      name,
      value,
    ]),
  )
}

const darkStart = stylesheet.indexOf(":root[data-theme='dark'] {")
const light = tokens(stylesheet.slice(0, darkStart))
const dark = {
  ...light,
  ...tokens(stylesheet.slice(darkStart, stylesheet.indexOf('}', darkStart))),
}

function luminance(color: string): number {
  const [red, green, blue] = [1, 3, 5]
    .map((index) => parseInt(color.slice(index, index + 2), 16) / 255)
    .map((channel) => (channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4))
  return 0.2126 * red + 0.7152 * green + 0.0722 * blue
}

function contrast(first: string, second: string): number {
  const [lighter, darker] = [luminance(first), luminance(second)].sort((a, b) => b - a)
  return (lighter + 0.05) / (darker + 0.05)
}

function tooFaint(theme: Record<string, string>): string[] {
  return PAIRS.filter(([color, background]) => !(color in theme) || !(background in theme))
    .map(([color, background]) => `${color} ou ${background} : token introuvable`)
    .concat(
      PAIRS.filter(
        ([color, background, threshold]) => contrast(theme[color], theme[background]) < threshold,
      ).map(
        ([color, background, threshold]) =>
          `${color} sur ${background} : ${contrast(theme[color], theme[background]).toFixed(2)} < ${threshold}`,
      ),
    )
}

describe('the contrasts of the design tokens (ADR 0058)', () => {
  it('reads both themes from the stylesheet', () => {
    expect(darkStart).toBeGreaterThan(0)
    expect(light.brand).toBe('#5b3df5')
    expect(dark.brand).not.toBe(light.brand)
  })

  it('keeps every text and control readable in the light theme', () => {
    expect(tooFaint(light)).toEqual([])
  })

  it('keeps every text and control readable in the dark theme', () => {
    expect(tooFaint(dark)).toEqual([])
  })
})
