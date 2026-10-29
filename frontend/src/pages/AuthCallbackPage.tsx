import { Link, Navigate } from 'react-router'
import { useSession } from '../auth/session'

/** Retour du fournisseur d'identité : la bibliothèque OIDC échange le code, puis on rouvre la page demandée. */
export function AuthCallbackPage() {
  const session = useSession()

  if (session.status === 'authenticated') {
    return <Navigate to={session.returnTo ?? '/'} replace />
  }
  if (session.status === 'anonymous') {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-bold">Connexion impossible</h1>
        {session.error && <p className="text-slate-600">{session.error}</p>}
        <Link to="/" className="text-indigo-600 hover:underline">
          Retour à l'accueil
        </Link>
      </section>
    )
  }
  return <p>Connexion…</p>
}
