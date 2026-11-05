import { NavLink, Outlet, useLocation } from 'react-router'
import { useProfile } from '../api/profile'
import { useProgress } from '../api/progress'
import { useSession } from '../auth/session'
import { ProgressBadge } from './ProgressBadge'
import { button } from './ui/button'

function navClass({ isActive }: { isActive: boolean }) {
  return isActive ? 'font-semibold text-brand' : 'text-ink-muted hover:text-brand'
}

function SessionControls() {
  const session = useSession()
  const location = useLocation()
  // Appelé dès la connexion : le premier appel crée le profil avec le fuseau du navigateur.
  const profile = useProfile(session.status === 'authenticated')
  const progress = useProgress(session.status === 'authenticated')

  if (session.status === 'loading') {
    return null
  }
  if (session.status === 'anonymous') {
    return (
      <button
        type="button"
        onClick={() => session.login(location.pathname + location.search)}
        className={button({ size: 'sm' })}
      >
        Se connecter
      </button>
    )
  }
  return (
    <div className="flex items-center gap-4">
      {progress.data && <ProgressBadge progress={progress.data} />}
      <NavLink to="/profile" className={navClass}>
        {profile.data?.displayName ?? session.userName ?? 'Mon profil'}
      </NavLink>
      <button
        type="button"
        onClick={session.logout}
        className="text-sm text-ink-muted hover:text-ink"
      >
        Se déconnecter
      </button>
    </div>
  )
}

export function Layout() {
  return (
    <div className="min-h-dvh bg-canvas text-ink">
      <header className="border-b border-line bg-surface">
        <nav className="mx-auto flex max-w-3xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3">
          <NavLink to="/" className="text-lg font-bold text-brand">
            Recallio
          </NavLink>
          <NavLink to="/review" className={navClass}>
            Réviser
          </NavLink>
          <NavLink to="/subjects" className={navClass}>
            Sujets
          </NavLink>
          <NavLink to="/groups" className={navClass}>
            Groupes
          </NavLink>
          <NavLink to="/ranked" className={navClass}>
            Classé
          </NavLink>
          <div className="ml-auto">
            <SessionControls />
          </div>
        </nav>
      </header>
      <main className="mx-auto max-w-3xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
