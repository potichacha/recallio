import type { ComponentType } from 'react'
import { Outlet, type RouteObject } from 'react-router'
import { RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { PageLoading } from './components/PageLoading'
import { AppError, NotFoundPage, PageError } from './components/RouteError'
import { HomePage } from './pages/HomePage'

/** Posé avant de recharger pour un morceau introuvable : on ne recharge qu'une fois. */
const RELOADED_KEY = 'recallio.reloaded-for-chunk'

/**
 * Charge le code d'une page. Après un déploiement, un onglet resté ouvert demande des fichiers qui n'existent plus :
 * on recharge alors l'application, une seule fois, plutôt que d'afficher une erreur.
 */
async function loaded<M>(load: () => Promise<M>): Promise<M> {
  try {
    const module = await load()
    sessionStorage.removeItem(RELOADED_KEY)
    return module
  } catch (error) {
    if (sessionStorage.getItem(RELOADED_KEY) !== null) throw error
    sessionStorage.setItem(RELOADED_KEY, 'true')
    window.location.reload()
    // La page se recharge : la navigation en cours n'a plus à aboutir.
    return new Promise<M>(() => {})
  }
}

/**
 * Une page chargée à la demande : son code n'est téléchargé qu'à la première visite (audit de novembre 2026,
 * élément 12). Seuls le layout et l'accueil sont dans le JavaScript de l'ouverture, que borne `npm run size`.
 */
function page<Name extends string, M extends Record<Name, ComponentType>>(
  load: () => Promise<M>,
  name: Name,
): Pick<RouteObject, 'lazy' | 'HydrateFallback'> {
  return {
    lazy: async () => ({ Component: (await loaded(load))[name] }),
    HydrateFallback: PageLoading,
  }
}

/** Les pages des comptes : robots.txt les écarte des moteurs de recherche (ADR 0065), ce que vérifie seo.test.ts. */
export const accountPages: RouteObject[] = [
  {
    path: 'subjects/:slug/course',
    ...page(() => import('./pages/LearningPathPage'), 'LearningPathPage'),
  },
  {
    path: 'subjects/:slug/practice/:notion',
    ...page(() => import('./practice/PracticePage'), 'PracticePage'),
  },
  {
    path: 'subjects/:slug/exam',
    ...page(() => import('./exam/MockExamPage'), 'MockExamPage'),
  },
  {
    path: 'subjects/:slug/mistakes',
    ...page(() => import('./practice/MistakesPage'), 'MistakesPage'),
  },
  {
    path: 'subjects/:slug/level-check',
    ...page(() => import('./level/LevelCheckPage'), 'LevelCheckPage'),
  },
  { path: 'review', ...page(() => import('./review/ReviewPage'), 'ReviewPage') },
  { path: 'ranked', ...page(() => import('./ranked/RankedPage'), 'RankedPage') },
  { path: 'ranked/duels/:id', ...page(() => import('./ranked/DuelPage'), 'DuelPage') },
  { path: 'groups', ...page(() => import('./groups/GroupsPage'), 'GroupsPage') },
  { path: 'groups/:groupId', ...page(() => import('./groups/GroupPage'), 'GroupPage') },
  {
    path: 'challenges/:challengeId',
    ...page(() => import('./challenges/ChallengePage'), 'ChallengePage'),
  },
  { path: 'live/:gameId', ...page(() => import('./live/LivePage'), 'LivePage') },
  { path: 'join/:token', ...page(() => import('./groups/JoinPage'), 'JoinPage') },
  { path: 'welcome', ...page(() => import('./pages/WelcomePage'), 'WelcomePage') },
  { path: 'profile/:section?', ...page(() => import('./pages/ProfilePage'), 'ProfilePage') },
  {
    path: 'admin/reports',
    ...page(() => import('./admin/ContentReportsPage'), 'ContentReportsPage'),
  },
  {
    path: 'admin/retention',
    ...page(() => import('./admin/RetentionPage'), 'RetentionPage'),
  },
  { path: 'admin/beta', ...page(() => import('./admin/BetaPage'), 'BetaPage') },
]

/** Les pages, dans le layout. seo.test.ts vérifie aussi ce que robots.txt laisse aux moteurs des pages publiques. */
export const pages: RouteObject[] = [
  { index: true, element: <HomePage /> },
  // Sans compte : le catalogue, le Tour (seul « Exécuter » demande de se connecter, ADR 0037), un profil public
  // (ADR 0043), l'essai (ADR 0050) et le défi du jour, qu'un lien partagé ouvre chez un visiteur (jouer demande un
  // compte, ADR 0067). Le plan du site du backend (SitemapService, ADR 0065) recopie les adresses de
  // l'accueil, des pages Cours et Tour, du catalogue, des pages légales, d'un sujet, de ses leçons et de son Tour : les
  // changer ici, c'est les changer là-bas.
  { path: 'learn', ...page(() => import('./subjects/CoursesPage'), 'CoursesPage') },
  { path: 'tours', ...page(() => import('./subjects/ToursPage'), 'ToursPage') },
  { path: 'play', ...page(() => import('./play/PlayPage'), 'PlayPage') },
  { path: 'subjects', ...page(() => import('./pages/SubjectsPage'), 'SubjectsPage') },
  { path: 'subjects/:slug', ...page(() => import('./pages/SubjectPage'), 'SubjectPage') },
  {
    path: 'subjects/:slug/course/:notion',
    ...page(() => import('./subjects/LessonPage'), 'LessonPage'),
  },
  {
    path: 'subjects/:slug/path',
    ...page(() => import('./subjects/PathToCourse'), 'PathToCourse'),
  },
  { path: 'themes/:slug', ...page(() => import('./pages/ThemePage'), 'ThemePage') },
  {
    path: 'subjects/:slug/tour',
    ...page(() => import('./tour/TourOutlinePage'), 'TourOutlinePage'),
  },
  {
    path: 'subjects/:slug/tour/:notion',
    ...page(() => import('./tour/TourNotionPage'), 'TourNotionPage'),
  },
  {
    path: 'u/:handle',
    ...page(() => import('./pages/PublicProfilePage'), 'PublicProfilePage'),
  },
  { path: 'try/:slug', ...page(() => import('./pages/TrialPage'), 'TrialPage') },
  // Les deux pages légales (ADR 0074), en français comme leur texte ; le plan du site les recopie aussi.
  {
    path: 'mentions-legales',
    ...page(() => import('./legal/LegalNoticePage'), 'LegalNoticePage'),
  },
  {
    path: 'confidentialite',
    ...page(() => import('./legal/PrivacyPolicyPage'), 'PrivacyPolicyPage'),
  },
  {
    path: 'daily/:subject',
    ...page(() => import('./daily/DailyChallengePage'), 'DailyChallengePage'),
  },
  {
    path: 'auth/callback',
    ...page(() => import('./pages/AuthCallbackPage'), 'AuthCallbackPage'),
  },
  {
    // Réservé aux utilisateurs connectés ; les autres sont invités à se connecter.
    element: (
      <RequireAuth>
        <Outlet />
      </RequireAuth>
    ),
    children: accountPages,
  },
  { path: '*', element: <NotFoundPage /> },
]

/** Routes de l'application, partagées par le routeur du navigateur et celui des tests. */
export const routes: RouteObject[] = [
  {
    element: <Layout />,
    // Le dernier filet, si le layout lui-même échoue.
    ErrorBoundary: AppError,
    // Le filet des pages (audit, élément 8) : le layout reste affiché autour du message.
    children: [{ ErrorBoundary: PageError, children: pages }],
  },
]
