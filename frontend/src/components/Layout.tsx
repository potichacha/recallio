import { NavLink, Outlet, useLocation } from 'react-router'
import { useProfile } from '../api/profile'
import { useProgress } from '../api/progress'
import { useSession } from '../auth/session'
import { ProgressBadge } from './ProgressBadge'

function navClass({ isActive }: { isActive: boolean }) {
  return isActive ? 'font-semibold text-indigo-600' : 'text-slate-600 hover:text-indigo-600'
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
        className="rounded-lg bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-700"
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
        className="text-sm text-slate-500 hover:text-slate-800"
      >
        Se déconnecter
      </button>
    </div>
  )
}

export function Layout() {
  return (
    <div className="min-h-dvh bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <nav className="mx-auto flex max-w-3xl items-center gap-6 px-4 py-3">
          <NavLink to="/" className="text-lg font-bold text-indigo-600">
            Recallio
          </NavLink>
          <NavLink to="/review" className={navClass}>
            Réviser
          </NavLink>
          <NavLink to="/subjects" className={navClass}>
            Sujets
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
