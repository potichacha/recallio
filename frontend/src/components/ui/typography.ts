/**
 * Les titres des maquettes « trois portes » (ADR 0077). Ces recettes ne donnent que la forme du texte : sa couleur
 * vient de l'endroit où il est posé (un bandeau, une carte), et sa balise de celui qui l'écrit (`h1`, `h2`, `legend`).
 */

/** Le surtitre : une ligne courte, en capitales espacées, au-dessus d'un titre. */
export function eyebrow(): string {
  return 'text-[0.8125rem] font-black tracking-[0.075rem] uppercase'
}

export type HeadingSize = 'lg' | 'md'

const HEADINGS: Record<HeadingSize, string> = {
  // Le titre d'un écran : 28 px.
  lg: 'text-[1.75rem] leading-[1.1] font-black tracking-[-0.025rem]',
  // Le titre d'une section : 19 px.
  md: 'text-[1.1875rem] font-black tracking-[-0.0125rem]',
}

export function heading({ size = 'lg' }: { size?: HeadingSize } = {}): string {
  return HEADINGS[size]
}
