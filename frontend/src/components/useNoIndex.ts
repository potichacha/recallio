import { useEffect } from 'react'

/**
 * Demande aux moteurs de recherche de ne pas indexer la page, tant qu'elle est affichée (ADR 0065). Une application
 * monopage répond 200 à toute adresse : sans cette balise, une page introuvable serait indexée comme une autre.
 */
export function useNoIndex() {
  useEffect(() => {
    const meta = document.createElement('meta')
    meta.name = 'robots'
    meta.content = 'noindex'
    document.head.append(meta)
    return () => meta.remove()
  }, [])
}
