import { usePageEnter } from './usePageEnter'
import { usePageTitle } from './usePageTitle'
import { OfflineBanner } from './states/OfflineBanner'
import { User } from '@phosphor-icons/react'
import { useRef, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigation } from 'react-router'
import { useProfile } from '../api/profile'
import { useProgress } from '../api/progress'
import { useSession } from '../auth/session'
import { DESTINATIONS, destinationOf } from './destinations'
import { DEFAULT_LAYOUT_MODE, LayoutModeContext, type LayoutMode } from './layoutMode'
import { ProgressBadge } from './ProgressBadge'
import { Rico } from './Rico'
import { button } from './ui/button'

/**
 * Le nom de l'application, qui ramène à l'accueil. Dans le menu latéral d'un ordinateur, il est en minuscules, comme
 * sur la maquette ; en tête d'un téléphone, il n'est là que pour un visiteur : connecté, la série, l'XP et l'avatar
 * prennent sa place, et l'onglet « Accueil » ramène à l'accueil.
 */
function Brand({ side = false }: { side?: boolean }) {
  return (
    <NavLink
      to="/"
      className={
        side
          ? 'hidden items-center gap-2.5 px-2.5 pb-4 text-2xl font-black tracking-[-0.025rem] text-ink lowercase md:flex'
          : 'flex items-center gap-2 text-lg font-black text-brand min-[360px]:text-[1.375rem] md:hidden'
      }
    >
      <Rico size={side ? 34 : 40} decorative />
      Recallio
    </NavLink>
  )
}

/** L'avatar, qui ouvre le profil : l'initiale du nom, dans un rond au contour de la marque. */
function Avatar({ name }: { name: string | undefined }) {
  const initial = name?.trim().charAt(0).toUpperCase()
  return (
    <NavLink
      to="/profile"
      title="Ton profil"
      aria-label={name ?? 'Ton profil'}
      className="flex size-11 shrink-0 items-center justify-center rounded-full border-[3px] border-brand bg-brand-soft text-[1.0625rem] font-black text-brand-strong"
    >
      {initial || <User weight="bold" aria-hidden className="size-5" />}
    </NavLink>
  )
}

/** Ce que l'en-tête montre de la session : la série, l'XP et l'avatar, ou « Se connecter » pour un visiteur. */
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
      <>
        <Brand />
        <button
          type="button"
          onClick={() => session.login(location.pathname + location.search)}
          className={`ml-auto ${button({ variant: 'quiet', size: 'md' })}`}
        >
          Se connecter
        </button>
      </>
    )
  }
  return (
    <>
      {progress.data && <ProgressBadge progress={progress.data} />}
      <span className="ml-auto md:ml-0">
        <Avatar name={profile.data?.displayName ?? session.userName} />
      </span>
    </>
  )
}

/** Une destination : icône pleine quand on y est, et toujours son nom écrit (une icône seule ne se devine pas). */
function Destination({ to, label, icon: Icon }: (typeof DESTINATIONS)[number]) {
  const isActive = destinationOf(useLocation().pathname) === to
  return (
    <Link
      to={to}
      aria-current={isActive ? 'page' : undefined}
      className={`flex flex-col items-center gap-0.5 rounded-xl py-2 text-[0.6875rem] min-[360px]:text-xs md:min-h-[3.125rem] md:flex-row md:gap-3 md:rounded-[1rem] md:px-3.5 md:py-0 md:text-base ${isActive ? 'bg-brand-soft font-black text-brand-strong' : 'font-extrabold text-ink-muted hover:text-brand-strong md:text-ink-soft'}`}
    >
      <Icon weight={isActive ? 'fill' : 'bold'} aria-hidden className="size-6 shrink-0" />
      {label}
    </Link>
  )
}

/**
 * La mise en page (ADR 0077) : sur un téléphone, un en-tête (la série, l'XP et l'avatar), la page, et une barre de
 * navigation qui flotte en bas de l'écran ; sur un ordinateur, un menu latéral à gauche de la page.
 */
export function Layout() {
  const [mode, setMode] = useState<LayoutMode>(DEFAULT_LAYOUT_MODE)
  // Le code d'une page se télécharge à sa première visite : la page quittée reste affichée en attendant.
  const loadingPage = useNavigation().state === 'loading'
  const page = usePageEnter()
  const main = useRef<HTMLElement>(null)
  usePageTitle(main)
  const width = mode.wide ? 'max-w-6xl' : 'max-w-3xl'
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
          className="sr-only focus-visible:not-sr-only focus-visible:absolute focus-visible:top-2 focus-visible:left-2 focus-visible:z-40 focus-visible:rounded-lg focus-visible:bg-surface focus-visible:px-3 focus-visible:py-2 focus-visible:font-bold focus-visible:text-brand-strong"
        >
          Aller au contenu
        </a>
        <OfflineBanner />
        {loadingPage && (
          <div
            role="progressbar"
            aria-label="Chargement de la page"
            className="fixed inset-x-0 top-0 z-30 h-0.5 bg-brand motion-safe:animate-pulse"
          />
        )}
        <div className="mx-auto md:flex md:max-w-[90rem] md:items-start md:gap-6 md:p-6">
          {/*
            Le menu latéral d'un ordinateur : le logo et les quatre destinations, dans une carte qui reste en vue. Sur
            un téléphone, ce bloc n'a pas de forme : le logo est masqué, et la navigation flotte en bas de l'écran.
          */}
          <div className="md:sticky md:top-6 md:flex md:h-[calc(100dvh-3rem)] md:w-52 md:shrink-0 md:flex-col md:overflow-y-auto md:rounded-3xl md:bg-surface md:px-3.5 md:py-5 md:shadow-relief lg:w-60">
            <Brand side />
            <nav
              aria-label="Navigation principale"
              className={`fixed inset-x-4 bottom-[calc(1rem+env(safe-area-inset-bottom))] z-20 mx-auto max-w-md grid-cols-4 gap-1 rounded-[1.625rem] border-2 border-line bg-surface p-1.5 shadow-float md:static md:mx-0 md:max-w-none md:flex-col md:rounded-none md:border-0 md:bg-transparent md:p-0 md:shadow-none ${mode.focus ? 'hidden md:flex' : 'grid md:flex'}`}
            >
              {DESTINATIONS.map((destination) => (
                <Destination key={destination.to} {...destination} />
              ))}
            </nav>
          </div>
          <div className="min-w-0 flex-1">
            <header
              className={`sticky top-0 z-20 min-h-14 items-center gap-2 bg-canvas px-5 py-1.5 md:static md:min-h-0 md:justify-end md:gap-3 md:bg-transparent md:px-0 md:pt-0 md:pb-5 ${mode.focus ? 'hidden md:flex' : 'flex'}`}
            >
              <SessionControls />
            </header>
            <main
              ref={main}
              id="contenu"
              tabIndex={-1}
              className={`mx-auto px-4 focus-visible:outline-none md:px-0 md:pt-0 ${width} ${mode.focus ? 'overflow-x-clip pt-4 pb-0 md:pb-10' : 'pt-2 pb-8'}`}
            >
              {/*
                Pendant une session, la carte suivante entre par la droite : `overflow-x-clip` l'empêche d'élargir la
                page, qui défilerait de côté sur un téléphone.
              */}
              {/* La page qui s'ouvre glisse en apparaissant. Elle n'est pas remontée : seule l'animation rejoue. */}
              <div ref={page}>
                <Outlet />
              </div>
            </main>
            {/*
              Le pied de page (ADR 0074) : les deux pages légales, sur toutes les pages. Pas en mode concentration (une
              session, un duel, un examen), où rien ne doit distraire. Sur un téléphone, sa marge du bas le tient
              au-dessus de la barre de navigation, qui flotte.
            */}
            {!mode.focus && (
              <footer className={`mx-auto px-4 pb-28 md:px-0 md:pb-2 ${width}`}>
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
        </div>
      </div>
    </LayoutModeContext.Provider>
  )
}
