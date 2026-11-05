import { Link } from 'react-router'
import { useApiStatus } from '../api/health'
import { RequireFollowedSubjects } from '../auth/RequireFollowedSubjects'
import { useSession } from '../auth/session'
import { DashboardPage } from '../dashboard/DashboardPage'
import { button } from '../components/ui/button'

/** Accueil : le tableau de bord pour un utilisateur connecté (après le choix de ses sujets), la présentation sinon. */
export function HomePage() {
  const session = useSession()
  return session.status === 'authenticated' ? (
    <RequireFollowedSubjects>
      <DashboardPage />
    </RequireFollowedSubjects>
  ) : (
    <Landing />
  )
}

function Landing() {
  const status = useApiStatus()

  return (
    <section className="flex flex-col gap-6 py-10">
      <h1 className="text-4xl font-bold text-brand">Recallio</h1>
      <p className="text-ink-muted">Révisez vos entretiens techniques, un peu chaque jour.</p>
      <div className="flex flex-wrap gap-3">
        <Link to="/review" className={button()}>
          Réviser maintenant
        </Link>
        <Link to="/subjects" className={button({ variant: 'secondary' })}>
          Parcourir les sujets
        </Link>
      </div>
      <p role="status" className="rounded-lg bg-surface-muted px-4 py-3 text-sm text-ink-soft">
        {status.isPending && 'API : vérification…'}
        {status.isSuccess && `API : ${status.data}`}
        {status.isError && 'API : injoignable'}
      </p>
    </section>
  )
}
