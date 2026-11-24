/**
 * Le cadre d'une carte de révision (ADR 0058). Sur un téléphone, il n'y en a pas : la carte occupe tout l'écran, sur le
 * fond d'une carte (voir `Layout`, mode concentration), et son action se cale en bas. Sur un écran large, c'est une
 * carte bordée, à la largeur de la page.
 */
export const CARD_FRAME =
  'flex flex-1 flex-col gap-3.5 md:flex-none md:gap-4 md:rounded-2xl md:border md:border-line md:bg-surface md:p-6 md:shadow-sm'

/** Le bas d'une carte : son action principale en pleine largeur sur un téléphone, à sa taille sur un écran large. */
export const CARD_ACTION = 'w-full md:w-auto md:min-w-56 md:self-start'
