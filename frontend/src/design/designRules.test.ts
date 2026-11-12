import { describe, expect, it } from 'vitest'

/**
 * Les règles du design system (ADR 0049, gardées par l'ADR 0058), vérifiées à chaque `npm test` : elles empêchent de retomber dans
 * les réflexes qui donnent l'air d'une interface générée (la palette par défaut de Tailwind, les dégradés, les émojis
 * en guise d'icônes). Les tests eux-mêmes ne sont pas concernés.
 */
const sources = import.meta.glob(
  ['../**/*.{ts,tsx}', '!../**/*.test.{ts,tsx}', '!../api/schema.d.ts'],
  { query: '?raw', import: 'default', eager: true },
) as Record<string, string>

const DEFAULT_PALETTE =
  /\b(?:bg|text|border|ring|fill|stroke|outline|divide|decoration|from|via|to|shadow|accent|caret|placeholder)-(?:slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose)-\d{2,3}\b/
const GRADIENT = /\bbg-(?:linear|radial|conic|gradient)-|create(?:Linear|Radial|Conic)Gradient/
const EMOJI = /\p{Extended_Pictographic}/u
// Une animation sans son garde-fou : `animate-…` qui ne suit ni `motion-safe:` ni `motion-reduce:`.
const UNGUARDED_ANIMATION = /(?<!motion-safe:|motion-reduce:)animate-[a-z]/
const HAND_WRITTEN_STATE = /<p[^>]*>\s*(?:Chargement|Impossible de charger)/

function offenders(pattern: RegExp) {
  return Object.entries(sources)
    .filter(([, text]) => pattern.test(text))
    .map(([path]) => path)
}

describe('design system', () => {
  it('reads the source files it checks', () => {
    expect(Object.keys(sources).length).toBeGreaterThan(50)
  })

  it('uses the tokens of the theme, never the default Tailwind palette', () => {
    expect(offenders(DEFAULT_PALETTE)).toEqual([])
  })

  it('draws no gradient', () => {
    expect(offenders(GRADIENT)).toEqual([])
  })

  it('uses real icons rather than emoji', () => {
    expect(offenders(EMOJI)).toEqual([])
  })

  // Le mouvement s'efface pour qui a demandé moins d'animations (ADR 0058) : chaque animation porte son garde-fou, en
  // plus du filet global de index.css.
  it('guards every animation with motion-safe or motion-reduce', () => {
    expect(offenders(UNGUARDED_ANIMATION)).toEqual([])
  })

  // Un écran qui charge ou qui a échoué passe par LoadingState et ErrorState (ADR 0058) : une silhouette et un bouton
  // « Réessayer », les mêmes partout, plutôt qu'une ligne de texte écrite à la main.
  it('shows loading and failure through the shared states, never a hand-written line', () => {
    const handWritten = Object.entries(sources)
      .filter(([path]) => !path.includes('/components/states/'))
      .filter(([, text]) => HAND_WRITTEN_STATE.test(text))
      .map(([path]) => path)
    expect(handWritten).toEqual([])
  })
})
