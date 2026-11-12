import { LoadingState } from '../components/states/LoadingState'
import type { ReactNode } from 'react'
import { useLocation } from 'react-router'
import { useSession } from './session'
import { button } from '../components/ui/button'

/** Réserve une page aux utilisateurs connectés ; les autres sont invités à se connecter. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const session = useSession()
  const location = useLocation()

  if (session.status === 'loading') {
    return <LoadingState label="Chargement…" />
  }
  if (session.status === 'anonymous') {
    return (
      <section className="flex flex-col items-start gap-4">
        <h1 className="text-2xl font-bold">Connexion requise</h1>
        <p className="text-ink-muted">Connecte-toi pour accéder à cette page.</p>
        <button
          type="button"
          onClick={() => session.login(location.pathname + location.search)}
          className={button()}
        >
          Se connecter
        </button>
      </section>
    )
  }
  return children
}
