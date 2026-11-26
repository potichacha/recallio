import { usePageEnter } from './usePageEnter'
import { usePageTitle } from './usePageTitle'
import { OfflineBanner } from './states/OfflineBanner'
import { BookOpenText, Cards, House, Path, Trophy, User, type Icon } from '@phosphor-icons/react'
import { useRef, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigation } from 'react-router'
import { useProfile } from '../api/profile'
import { useProgress } from '../api/progress'
import { useSession } from '../auth/session'
import { DEFAULT_LAYOUT_MODE, LayoutModeContext, type LayoutMode } from './layoutMode'
import { ProgressBadge } from './ProgressBadge'
import { Rico } from './Rico'
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
    matches: (path) => path.startsWith('/review') || inSubject('practice|exam|mistakes').test(path),
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

/** Le nom de l'application, qui ramène à l'accueil ; pour un visiteur, Rico l'accompagne, comme sur la page publique. */
function Brand() {
  const session = useSession()
  return (
    <NavLink
      to="/"
      className="flex items-center gap-2 text-lg font-black text-brand min-[360px]:text-[1.375rem] md:text-xl"
    >
      {session.status === 'anonymous' && <Rico size={40} decorative />}
      Recallio
    </NavLink>
  )
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
        className={button({ variant: 'quiet', size: 'md' })}
      >
        Se connecter
      </button>
    )
  }
  const name = profile.data?.displayName ?? session.userName ?? 'Mon profil'
  return (
    <div className="flex items-center gap-1.5 min-[360px]:gap-2 md:gap-3">
      {progress.data && <ProgressBadge progress={progress.data} />}
      <NavLink
        to="/profile"
        aria-label={name}
        className={({ isActive }) =>
          `flex min-h-11 items-center gap-2 rounded-full text-sm font-extrabold md:pr-1 ${isActive ? 'text-brand' : 'text-ink-soft hover:text-brand'}`
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
      className={`flex min-h-14 flex-col items-center justify-center gap-0.5 text-xs font-extrabold md:min-h-11 md:flex-row md:gap-2 md:rounded-lg md:px-3 md:text-sm ${isActive ? 'text-brand md:bg-brand-soft' : 'text-ink-muted hover:text-brand'}`}
    >
      <Icon weight={isActive ? 'fill' : 'bold'} aria-hidden className="size-6 md:size-5" />
      {label}
    </Link>
  )
}

export function Layout() {
  const [mode, setMode] = useState<LayoutMode>(DEFAULT_LAYOUT_MODE)
  // Le code d'une page se télécharge à sa première visite : la page quittée reste affichée en attendant.
  const loadingPage = useNavigation().state === 'loading'
  const page = usePageEnter()
  const main = useRef<HTMLElement>(null)
  usePageTitle(main)
  return (
    <LayoutModeContext.Provider value={setMode}>
      {/*
        Pendant une session (mode concentration), sur un téléphone, la carte occupe tout l'écran, sur le fond d'une
        carte : l'en-tête et la navigation s'effacent.
      */}
      <div className={`min-h-dvh text-ink ${mode.focus ? 'bg-surface md:bg-canvas' : 'bg-canvas'}`}>
        {/* Le premier arrêt du clavier (WCAG 2.4.1) : sauter la navigation, répétée sur chaque page. */}
        <a
          href="#contenu"
          className="sr-only focus-visible:not-sr-only focus-visible:absolute focus-visible:top-2 focus-visible:left-2 focus-visible:z-30 focus-visible:rounded-lg focus-visible:bg-surface focus-visible:px-3 focus-visible:py-2 focus-visible:font-bold focus-visible:text-brand-strong"
        >
          Aller au contenu
        </a>
        <OfflineBanner />
        <header
          className={`sticky top-0 z-20 bg-canvas md:border-b md:border-line md:bg-surface ${mode.focus ? 'hidden md:block' : ''}`}
        >
          <div className="mx-auto flex max-w-6xl items-center gap-2 px-4 py-2 md:gap-4">
            <Brand />
            <nav
              aria-label="Navigation principale"
              className={`fixed inset-x-0 bottom-0 z-20 grid-cols-5 border-t-2 border-line bg-surface pb-[env(safe-area-inset-bottom)] md:static md:flex md:gap-1 md:border-0 md:bg-transparent md:pb-0 ${mode.focus ? 'hidden md:flex' : 'grid'}`}
            >
              {DESTINATIONS.map((destination) => (
                <Destination key={destination.to} {...destination} />
              ))}
            </nav>
            <div className="ml-auto">
              <SessionControls />
            </div>
          </div>
          {loadingPage && (
            <div
              role="progressbar"
              aria-label="Chargement de la page"
              className="absolute inset-x-0 bottom-0 h-0.5 bg-brand motion-safe:animate-pulse"
            />
          )}
        </header>
        <main
          ref={main}
          id="contenu"
          tabIndex={-1}
          className={`mx-auto px-4 focus-visible:outline-none md:pt-6 ${mode.wide ? 'max-w-6xl' : 'max-w-3xl'} ${mode.focus ? 'overflow-x-clip pt-4 pb-0 md:pb-10' : 'pt-2 pb-8'}`}
        >
          {/*
            Pendant une session, la carte suivante entre par la droite : `overflow-x-clip` l'empêche d'élargir la page,
            qui défilerait de côté sur un téléphone.
          */}
          {/* La page qui s'ouvre glisse en apparaissant. Elle n'est pas remontée : seule l'animation rejoue. */}
          <div ref={page}>
            <Outlet />
          </div>
        </main>
        {/*
          Le pied de page (ADR 0074) : les deux pages légales, sur toutes les pages. Pas en mode concentration (une
          session, un duel, un examen), où rien ne doit distraire. Sur un téléphone, sa marge du bas le tient au-dessus
          de la barre de navigation.
        */}
        {!mode.focus && (
          <footer className={`mx-auto px-4 pb-28 md:pb-8 ${mode.wide ? 'max-w-6xl' : 'max-w-3xl'}`}>
            <nav
              aria-label="Informations légales"
              className="flex flex-wrap gap-x-5 gap-y-1 border-t border-line pt-4 text-sm text-ink-muted"
            >
              <Link to="/mentions-legales" className="py-1 hover:text-brand hover:underline">
                Mentions légales
              </Link>
              <Link to="/confidentialite" className="py-1 hover:text-brand hover:underline">
                Confidentialité
              </Link>
            </nav>
          </footer>
        )}
      </div>
    </LayoutModeContext.Provider>
  )
}
