import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { vi } from 'vitest'
import { SessionContext, type Session } from '../auth/session'
import { routes } from '../routes'

/** Session factice : anonyme par défaut, avec des fonctions espionnes pour login et logout. */
export function fakeSession(overrides: Partial<Session> = {}): Session {
  return {
    status: 'anonymous',
    login: vi.fn(),
    logout: vi.fn(),
    deleteIdentity: vi.fn(),
    ...overrides,
  }
}

/** Affiche l'application complète (routes et layout) à l'URL donnée, avec un cache de requêtes neuf. */
export function renderAt(path: string, session: Session = fakeSession()) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <SessionContext.Provider value={session}>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </SessionContext.Provider>,
  )
  return router
}

type StubResponse = { status?: number; body: unknown }

export const TEST_PROFILE = {
  id: 'u1',
  displayName: 'Alice',
  timezone: 'Europe/Paris',
  roles: [],
  followedSubjects: ['java'],
  tracks: { java: 'PREPARE' },
}
export const TEST_PROGRESS = {
  totalXp: 120,
  streak: 3,
  streakFreezes: 1,
  daysToNextFreeze: 4,
  freezeEarnedToday: false,
  cardsToday: 4,
  dailyGoal: 10,
  goalReachedToday: false,
  subjects: [{ slug: 'java', xp: 340, level: 3, levelXp: 300, nextLevelXp: 600 }],
}

function testSubject(slug: string, title: string, overrides: Record<string, unknown> = {}) {
  return {
    id: slug,
    slug,
    title,
    category: 'LANGUAGES',
    position: 10,
    status: 'PUBLISHED',
    themeCount: 2,
    cardCount: 20,
    hasTour: false,
    ...overrides,
  }
}

/** Le catalogue des tests : Java et Go publiés, avec leur Tour ; Rust en brouillon (visible des seuls admins). */
export const TEST_SUBJECTS = [
  testSubject('java', 'Java', { hasTour: true }),
  testSubject('go', 'Go', { position: 20, hasTour: true }),
  testSubject('rust', 'Rust', { position: 30, status: 'DRAFT' }),
]

/** Les réglages d'un groupe neuf : aucun sujet choisi. */
export const TEST_GROUP_SETTINGS = {
  subjects: [] as string[],
  themes: [] as string[],
  cardTypes: [] as string[],
  dailyGoal: 10,
  leaderboardVisibility: 'NAMED',
}

/** Le classement de la semaine d'un groupe dont personne n'a encore révisé. */
export const TEST_LEADERBOARD = {
  weekStart: '2026-10-26',
  weekEnd: '2026-11-01',
  dailyGoal: 10,
  visibility: 'NAMED',
  entries: [] as unknown[],
}

/** Ce que l'en-tête charge dès qu'un utilisateur est connecté : son profil et sa progression. */
export const SIGNED_IN_API: Record<string, StubResponse> = {
  '/api/v1/me': { body: TEST_PROFILE },
  '/api/v1/me/progress': { body: TEST_PROGRESS },
}

/**
 * Remplace fetch. Chaque requête est résolue par sa clé : `"PUT /api/v1/me"`, ou seulement le chemin
 * (`"/api/v1/themes"`) pour un GET. Une requête non prévue échoue, pour qu'un appel oublié se voie dans le test.
 */
export function stubApi(responses: Record<string, StubResponse>) {
  const fetchMock = vi.fn(async (request: Request) => {
    const { pathname } = new URL(request.url)
    const stub =
      responses[`${request.method} ${pathname}`] ??
      (request.method === 'GET' ? responses[pathname] : undefined)
    if (stub === undefined) {
      throw new Error(`Appel API non prévu dans le test : ${request.method} ${pathname}`)
    }
    // Une réponse 204 n'a pas de corps : le constructeur de Response le refuserait.
    if (stub.status === 204) return new Response(null, { status: 204 })
    return Response.json(stub.body, { status: stub.status ?? 200 })
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** Les requêtes reçues par le faux fetch, pour vérifier ce que l'application a envoyé. */
export function sentRequests(fetchMock: ReturnType<typeof stubApi>): Request[] {
  return fetchMock.mock.calls.map(([request]) => request)
}
