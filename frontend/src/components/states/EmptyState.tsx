import type { ReactNode } from 'react'
import { Rico, type RicoPose } from '../Rico'

/**
 * Un écran qui n'a encore rien à montrer (ADR 0058) : une première visite, ou une liste vide. Rico dit à quoi sert
 * l'écran, et `children` porte l'action pour commencer, s'il y en a une.
 */
export function EmptyState({
  title,
  text,
  pose = 'neutral',
  children,
}: {
  title: string
  text?: string
  pose?: RicoPose
  children?: ReactNode
}) {
  return (
    <div className="flex items-center gap-3 rounded-2xl border border-line bg-surface p-4 shadow-sm">
      <Rico pose={pose} size={64} decorative />
      <div className="flex flex-1 flex-col items-start gap-2">
        <p className="font-bold">{title}</p>
        {text && <p className="text-sm text-ink-muted">{text}</p>}
        {children}
      </div>
    </div>
  )
}
