import { createContext, useContext, useEffect } from 'react'

/**
 * Ce qu'une page demande à la mise en page (ADR 0050) : le mode concentration masque la barre de navigation pendant une
 * session, un duel ou un examen ; le mode large donne de la place au Tour, en deux colonnes.
 */
export type LayoutMode = { focus: boolean; wide: boolean }

export const DEFAULT_LAYOUT_MODE: LayoutMode = { focus: false, wide: false }

export const LayoutModeContext = createContext<(mode: LayoutMode) => void>(() => {})

/** Demande un mode de mise en page tant que la page est affichée ; il est rendu en la quittant. */
export function useLayoutMode({ focus = false, wide = false }: Partial<LayoutMode>) {
  const setMode = useContext(LayoutModeContext)
  useEffect(() => {
    setMode({ focus, wide })
    return () => setMode(DEFAULT_LAYOUT_MODE)
  }, [setMode, focus, wide])
}
