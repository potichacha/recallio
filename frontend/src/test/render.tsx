import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { vi } from 'vitest'
import { SessionContext, type Session } from '../auth/session'
import { routes } from '../routes'

/** Session factice : anonyme par défaut, avec des fonctions espionnes pour login et logout. */
export function fakeSession(overrides: Partial<Session> = {}): Session {
  return { status: 'anonymous', login: vi.fn(), logout: vi.fn(), ...overrides }
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

export const TEST_PROFILE = { id: 'u1', displayName: 'Alice', timezone: 'Europe/Paris', roles: [] }
export const TEST_PROGRESS = {
  totalPoints: 120,
  streak: 3,
  cardsToday: 4,
  dailyGoal: 10,
  goalReachedToday: false,
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
    return Response.json(stub.body, { status: stub.status ?? 200 })
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** Les requêtes reçues par le faux fetch, pour vérifier ce que l'application a envoyé. */
export function sentRequests(fetchMock: ReturnType<typeof stubApi>): Request[] {
  return fetchMock.mock.calls.map(([request]) => request)
}
