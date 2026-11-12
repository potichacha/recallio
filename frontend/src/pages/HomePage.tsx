import { lazy, Suspense } from 'react'
import { RequireFollowedSubjects } from '../auth/RequireFollowedSubjects'
import { useSession } from '../auth/session'
import { PageLoading } from '../components/PageLoading'

// Un visiteur ne télécharge pas le tableau de bord, ni un utilisateur connecté la page publique.
const DashboardPage = lazy(() =>
  import('../dashboard/DashboardPage').then((module) => ({ default: module.DashboardPage })),
)
const LandingPage = lazy(() =>
  import('./LandingPage').then((module) => ({ default: module.LandingPage })),
)

/** Accueil : le tableau de bord pour un utilisateur connecté (après le choix de ses sujets), la page publique sinon. */
export function HomePage() {
  const session = useSession()
  if (session.status === 'loading') return <PageLoading />
  return (
    <Suspense fallback={<PageLoading />}>
      {session.status === 'authenticated' ? (
        <RequireFollowedSubjects>
          <DashboardPage />
        </RequireFollowedSubjects>
      ) : (
        <LandingPage />
      )}
    </Suspense>
  )
}
