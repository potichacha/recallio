import { useEffect, type RefObject } from 'react'

const APP_NAME = 'Recallio'

/**
 * Donne à l'onglet le titre de la page affichée (WCAG 2.4.2, ADR 0058) : « Le Tour de Go · Recallio ». Un lecteur
 * d'écran l'annonce à chaque changement de page, et on retrouve l'onglet parmi d'autres. Le titre suit le premier titre
 * de niveau 1 de la page, même quand il arrive après un chargement : aucune page n'a à le déclarer.
 */
export function usePageTitle(main: RefObject<HTMLElement | null>) {
  useEffect(() => {
    const element = main.current
    if (!element) return
    const update = () => {
      const heading = element.querySelector('h1')?.textContent?.trim()
      document.title = heading ? `${heading} · ${APP_NAME}` : APP_NAME
    }
    update()
    const observer = new MutationObserver(update)
    observer.observe(element, { childList: true, subtree: true, characterData: true })
    return () => observer.disconnect()
  }, [main])
}
