import type { ReactNode } from 'react'
import { useLocation } from 'react-router'
import { useSession } from './session'

/** Réserve une page aux utilisateurs connectés ; les autres sont invités à se connecter. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const session = useSession()
  const location = useLocation()

  if (session.status === 'loading') {
    return <p>Chargement…</p>
  }
  if (session.status === 'anonymous') {
    return (
      <section className="flex flex-col items-start gap-4">
        <h1 className="text-2xl font-bold">Connexion requise</h1>
        <p className="text-slate-600">Connectez-vous pour accéder à cette page.</p>
        <button
          type="button"
          onClick={() => session.login(location.pathname + location.search)}
          className="rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700"
        >
          Se connecter
        </button>
      </section>
    )
  }
  return children
}
