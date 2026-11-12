import { useEffect, useRef } from 'react'
import { useLocation } from 'react-router'

const ENTER: Keyframe[] = [
  { opacity: 0, transform: 'translateY(8px)' },
  { opacity: 1, transform: 'none' },
]
const DURATION_MS = 220

/**
 * Fait glisser la page qui s'ouvre (ADR 0058) : à chaque changement d'adresse, son conteneur rejoue une courte
 * animation. Rien n'est remonté, donc une page qui garde son état d'une adresse à l'autre (le Tour) ne le perd pas.
 * Sans mouvement pour qui en demande moins, ni là où le navigateur ne sait pas animer.
 */
export function usePageEnter() {
  const ref = useRef<HTMLDivElement>(null)
  const { pathname } = useLocation()
  useEffect(() => {
    const reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? true
    if (!reduced) ref.current?.animate?.(ENTER, { duration: DURATION_MS, easing: 'ease-out' })
  }, [pathname])
  return ref
}
