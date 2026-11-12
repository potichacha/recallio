import { WifiSlash } from '@phosphor-icons/react'
import { useSyncExternalStore } from 'react'

function subscribe(onChange: () => void) {
  window.addEventListener('online', onChange)
  window.addEventListener('offline', onChange)
  return () => {
    window.removeEventListener('online', onChange)
    window.removeEventListener('offline', onChange)
  }
}

/** Vrai tant que le navigateur se dit hors ligne. Il peut se dire en ligne sans réseau réel : ce n'est qu'une aide. */
function useOffline() {
  return useSyncExternalStore(
    subscribe,
    () => !navigator.onLine,
    () => false,
  )
}

/**
 * Le bandeau « hors ligne » (ADR 0058), un seul pour toute l'application : il dit ce qui marche encore et ce qui ne
 * marche plus, et disparaît au retour du réseau. Il ne promet pas que les réponses partiront plus tard : ce n'est vrai
 * que si la page reste ouverte.
 */
export function OfflineBanner() {
  const offline = useOffline()
  if (!offline) return null
  return (
    <p
      role="status"
      className="flex items-center justify-center gap-2 border-b border-warning-line bg-warning-soft px-4 py-2 text-sm font-semibold"
    >
      <WifiSlash weight="bold" aria-hidden className="size-5 shrink-0 text-warning" />
      Tu es hors ligne : ce qui est déjà ouvert reste lisible, mais il faut le réseau pour répondre.
    </p>
  )
}
