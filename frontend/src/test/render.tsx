import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { vi } from 'vitest'
import { routes } from '../routes'

/** Affiche l'application complète (routes et layout) à l'URL donnée, avec un cache de requêtes neuf. */
export function renderAt(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return router
}

type StubResponse = { status?: number; body: unknown }

/**
 * Remplace fetch : chaque requête est résolue par son chemin (`/api/v1/themes`). Une requête non prévue échoue, pour
 * qu'un appel oublié se voie dans le test.
 */
export function stubApi(responses: Record<string, StubResponse>) {
  const fetchMock = vi.fn(async (request: Request) => {
    const { pathname } = new URL(request.url)
    const stub = responses[pathname]
    if (stub === undefined) {
      throw new Error(`Appel API non prévu dans le test : ${pathname}`)
    }
    return Response.json(stub.body, { status: stub.status ?? 200 })
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}
