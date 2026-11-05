/**
 * Les boutons du design system « Coach » (ADR 0049) : une seule recette pour un `<button>` comme pour un `<Link>`. Le
 * bouton principal a un bord épais en dessous et s'enfonce au clic ; sans mouvement pour qui en demande moins.
 */
export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'success'
export type ButtonSize = 'md' | 'sm'

const BASE =
  'inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition-[transform,box-shadow,background-color] duration-100 active:translate-y-0.5 disabled:pointer-events-none disabled:opacity-50 motion-reduce:transition-none motion-reduce:active:translate-y-0'

const VARIANTS: Record<ButtonVariant, string> = {
  primary: 'bg-brand text-on-brand shadow-press hover:bg-brand-strong active:shadow-none',
  secondary:
    'border-2 border-brand-line bg-surface text-brand shadow-press-soft hover:bg-brand-soft active:shadow-none',
  ghost: 'text-ink-muted hover:bg-surface-muted hover:text-ink',
  success: 'bg-success text-on-success hover:bg-success-strong',
}

const SIZES: Record<ButtonSize, string> = {
  md: 'px-4 py-2',
  sm: 'px-3 py-1.5 text-sm',
}

export function button({
  variant = 'primary',
  size = 'md',
}: { variant?: ButtonVariant; size?: ButtonSize } = {}): string {
  return `${BASE} ${VARIANTS[variant]} ${SIZES[size]}`
}
