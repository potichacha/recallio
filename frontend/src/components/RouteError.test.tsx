import { focusManager } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { createMemoryRouter, Outlet, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderAt, sentRequests, stubApi, TEST_SUBJECTS } from '../test/render'
import { PageError } from './RouteError'

function BrokenPage(): never {
  throw new Error('Une page qui casse')
}

describe('the safety net of the pages (audit, item 8)', () => {
  it('shows a message and a way out when a page throws, the layout staying in place', async () => {
    // React et le filet journalisent l'erreur attrapée : attendu ici.
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const router = createMemoryRouter([
      {
        element: (
          <>
            <nav aria-label="Navigation principale" />
            <Outlet />
          </>
        ),
        children: [
          { ErrorBoundary: PageError, children: [{ index: true, Component: BrokenPage }] },
        ],
      },
    ])

    render(<RouterProvider router={router} />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Quelque chose s’est mal passé')
    expect(screen.getByRole('button', { name: 'Recharger' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Revenir à l’accueil' })).toHaveAttribute('href', '/')
    expect(screen.getByRole('navigation', { name: 'Navigation principale' })).toBeInTheDocument()
  })

  it('says that an unknown address leads nowhere', async () => {
    renderAt('/nulle-part')

    expect(await screen.findByRole('heading', { name: 'Page introuvable' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Navigation principale' })).toBeInTheDocument()
  })
})

describe('the freshness of the content (audit, item 10)', () => {
  afterEach(() => focusManager.setFocused(undefined))

  it('does not ask for the catalogue again when the user comes back to the tab', async () => {
    const fetchMock = stubApi({ '/api/v1/subjects': { body: TEST_SUBJECTS } })
    renderAt('/subjects')
    await screen.findByRole('link', { name: /^Java/ })
    const catalogueRequests = () =>
      sentRequests(fetchMock).filter((request) => request.url.endsWith('/api/v1/subjects')).length
    const before = catalogueRequests()

    focusManager.setFocused(false)
    focusManager.setFocused(true)

    // Laisse à une requête le temps de partir, s'il devait y en avoir une : rien d'autre ne signale son absence.
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(catalogueRequests()).toBe(before)
  })
})
