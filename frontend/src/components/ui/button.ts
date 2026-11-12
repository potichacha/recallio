/**
 * Les boutons du design system « Rico » (ADR 0058) : une seule recette pour un `<button>` comme pour un `<Link>`. Épais
 * et ronds, le principal et le secondaire ont un bord en relief en dessous et s'enfoncent au clic ; sans mouvement
 * pour qui en demande moins. Une cible fait au moins 44 px de haut.
 */
export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'success' | 'danger'
export type ButtonSize = 'md' | 'sm'

const BASE =
  'inline-flex items-center justify-center gap-2 rounded-xl font-extrabold transition-[transform,box-shadow,background-color] duration-100 active:translate-y-0.5 disabled:pointer-events-none disabled:opacity-50 motion-reduce:transition-none motion-reduce:active:translate-y-0'

const VARIANTS: Record<ButtonVariant, string> = {
  primary: 'bg-brand text-on-brand shadow-press hover:bg-brand-strong active:shadow-none',
  secondary:
    'border-2 border-brand-line bg-surface text-brand shadow-press-soft hover:bg-brand-soft active:shadow-none',
  ghost: 'text-ink-muted hover:bg-surface-muted hover:text-ink',
  success: 'bg-success text-on-success hover:bg-success-strong',
  // Une action qui ne se rattrape pas : en contour, pour ne pas attirer le clic.
  danger: 'border-2 border-danger-line bg-surface text-danger-strong hover:bg-danger-soft',
}

const SIZES: Record<ButtonSize, string> = {
  md: 'min-h-11 px-5 py-2',
  sm: 'min-h-9 px-3 py-1.5 text-sm',
}

export function button({
  variant = 'primary',
  size = 'md',
}: { variant?: ButtonVariant; size?: ButtonSize } = {}): string {
  return `${BASE} ${VARIANTS[variant]} ${SIZES[size]}`
}
