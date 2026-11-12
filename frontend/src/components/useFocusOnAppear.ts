import { useEffect, useRef } from 'react'

/**
 * Donne le focus à un élément quand il apparaît (ADR 0058) : après « Valider », c'est « Carte suivante », pour
 * enchaîner les cartes au clavier sans chercher le bouton. À ne pas utiliser sous un chrono (live, duel), où un focus
 * qui se déplace ferait valider par mégarde.
 */
export function useFocusOnAppear<T extends HTMLElement>(shown: boolean) {
  const ref = useRef<T>(null)
  useEffect(() => {
    if (shown) ref.current?.focus()
  }, [shown])
  return ref
}
