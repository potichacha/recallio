import { BookOpenText, Cards, House, Path, Trophy, User, type Icon } from '@phosphor-icons/react'
import { useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router'
import { useProfile } from '../api/profile'
import { useProgress } from '../api/progress'
import { useSession } from '../auth/session'
import { DEFAULT_LAYOUT_MODE, LayoutModeContext, type LayoutMode } from './layoutMode'
import { ProgressBadge } from './ProgressBadge'
import { button } from './ui/button'

/** Une page d'un sujet, sous l'une des destinations : `/subjects/java/course/...` est dans le Cours. */
const inSubject = (sections: string) => new RegExp(`^/subjects/[^/]+/(${sections})(/|$)`)

/**
 * Les cinq destinations de l'application (ADR 0050, 0052) : en bas de l'écran sur un téléphone, à portée de pouce, et
 * en haut sur un ordinateur. Chacune reste allumée sur les pages qu'elle ouvre ; le profil est à part, en haut à droite.
 */
const DESTINATIONS: {
  to: string
  label: string
  icon: Icon
  matches: (path: string) => boolean
}[] = [
  { to: '/', label: 'Accueil', icon: House, matches: (path) => path === '/' },
  {
    to: '/learn',
    label: 'Cours',
    icon: BookOpenText,
    matches: (path) => path.startsWith('/learn') || inSubject('course').test(path),
  },
  {
    to: '/tours',
    label: 'Tour',
    icon: Path,
    matches: (path) => path.startsWith('/tours') || inSubject('tour').test(path),
  },
  {
    to: '/review',
    label: 'Révisions',
    icon: Cards,
    matches: (path) => path.startsWith('/review') || inSubject('practice|exam').test(path),
  },
  {
    to: '/play',
    label: 'Jouer',
    icon: Trophy,
    matches: (path) =>
      ['/play', '/ranked', '/groups', '/challenges', '/live', '/join'].some((prefix) =>
        path.startsWith(prefix),
      ),
  },
]

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
  const name = profile.data?.displayName ?? session.userName ?? 'Mon profil'
  return (
    <div className="flex items-center gap-3">
      {progress.data && <ProgressBadge progress={progress.data} />}
      <NavLink
        to="/profile"
        aria-label={name}
        className={({ isActive }) =>
          `flex min-h-11 items-center gap-2 rounded-full pr-1 text-sm font-medium ${isActive ? 'text-brand' : 'text-ink-soft hover:text-brand'}`
        }
      >
        <span className="flex size-9 items-center justify-center rounded-full bg-brand-soft text-brand">
          <User weight="bold" aria-hidden className="size-5" />
        </span>
        <span className="hidden max-w-40 truncate lg:inline">{name}</span>
      </NavLink>
    </div>
  )
}

/** Une destination : icône pleine quand on y est, et toujours son nom écrit (une icône seule ne se devine pas). */
function Destination({ to, label, icon: Icon, matches }: (typeof DESTINATIONS)[number]) {
  const isActive = matches(useLocation().pathname)
  return (
    <Link
      to={to}
      aria-current={isActive ? 'page' : undefined}
      className={`flex min-h-14 flex-col items-center justify-center gap-0.5 text-xs font-medium md:min-h-11 md:flex-row md:gap-2 md:rounded-lg md:px-3 md:text-sm ${isActive ? 'text-brand md:bg-brand-soft' : 'text-ink-muted hover:text-brand'}`}
    >
      <Icon weight={isActive ? 'fill' : 'bold'} aria-hidden className="size-6 md:size-5" />
      {label}
    </Link>
  )
}

export function Layout() {
  const [mode, setMode] = useState<LayoutMode>(DEFAULT_LAYOUT_MODE)
  return (
    <LayoutModeContext.Provider value={setMode}>
      <div className="min-h-dvh bg-canvas text-ink">
        <header className="sticky top-0 z-20 border-b border-line bg-surface">
          <div className="mx-auto flex max-w-6xl items-center gap-4 px-4 py-2">
            <NavLink to="/" className="text-xl font-bold text-brand">
              Recallio
            </NavLink>
            <nav
              aria-label="Navigation principale"
              className={`fixed inset-x-0 bottom-0 z-20 grid-cols-5 border-t border-line bg-surface pb-[env(safe-area-inset-bottom)] md:static md:flex md:gap-1 md:border-0 md:bg-transparent md:pb-0 ${mode.focus ? 'hidden md:flex' : 'grid'}`}
            >
              {DESTINATIONS.map((destination) => (
                <Destination key={destination.to} {...destination} />
              ))}
            </nav>
            <div className="ml-auto">
              <SessionControls />
            </div>
          </div>
        </header>
        <main
          className={`mx-auto px-4 pt-6 ${mode.wide ? 'max-w-6xl' : 'max-w-3xl'} ${mode.focus ? 'pb-10' : 'pb-28 md:pb-10'}`}
        >
          <Outlet />
        </main>
      </div>
    </LayoutModeContext.Provider>
  )
}
