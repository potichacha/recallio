import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderAt, stubApi, TEST_SUBJECTS } from '../../test/render'
import { EmptyState } from './EmptyState'
import { ErrorState } from './ErrorState'
import { LoadingState } from './LoadingState'
import { OfflineBanner } from './OfflineBanner'

/** Le navigateur se dit en ligne ou hors ligne, puis l'annonce comme il le fait à un changement de réseau. */
function network(online: boolean) {
  vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(online)
  act(() => {
    window.dispatchEvent(new Event(online ? 'online' : 'offline'))
  })
}

afterEach(() => vi.restoreAllMocks())

describe('the states of a screen (ADR 0058)', () => {
  it('tells screen readers what is loading, behind a silent silhouette', () => {
    const { container } = render(<LoadingState label="Chargement des sujets…" shape="tiles" />)

    expect(screen.getByText('Chargement des sujets…')).toHaveClass('sr-only')
    expect(container.firstChild).toHaveAttribute('aria-busy', 'true')
    const silhouette = container.querySelector('[aria-hidden="true"]')
    expect(silhouette?.querySelectorAll('.bg-surface-sunken')).toHaveLength(6)
    // Elle n'apparaît qu'après un court délai, et ne pulse pas pour qui a demandé moins d'animations.
    expect(silhouette).toHaveClass('motion-safe:animate-skeleton', 'motion-reduce:animate-appear')
  })

  it('says what failed and offers to retry', async () => {
    const retry = vi.fn()
    render(<ErrorState message="Impossible de charger les sujets." onRetry={retry} />)

    expect(screen.getByRole('alert')).toHaveTextContent('Impossible de charger les sujets.')
    await userEvent.click(screen.getByRole('button', { name: 'Réessayer' }))
    expect(retry).toHaveBeenCalledOnce()
  })

  it('offers no retry when there is nothing to retry', () => {
    render(<ErrorState message="Impossible de charger les sujets." />)

    expect(screen.queryByRole('button', { name: 'Réessayer' })).not.toBeInTheDocument()
  })

  it('says what an empty screen is for, with the action to start', () => {
    render(
      <EmptyState title="Tu ne fais partie d’aucun groupe." text="Un groupe révise ensemble.">
        <a href="/groups">Créer un groupe</a>
      </EmptyState>,
    )

    expect(screen.getByText('Tu ne fais partie d’aucun groupe.')).toBeInTheDocument()
    expect(screen.getByText('Un groupe révise ensemble.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Créer un groupe' })).toBeInTheDocument()
  })

  it('says when the browser is offline, and stops when the network comes back', () => {
    render(<OfflineBanner />)
    expect(screen.queryByRole('status')).not.toBeInTheDocument()

    network(false)
    expect(screen.getByRole('status')).toHaveTextContent('Tu es hors ligne')

    network(true)
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
})

describe('a screen that failed to load (ADR 0058)', () => {
  it('loads again on « Réessayer », without reloading the page', async () => {
    const responses: Parameters<typeof stubApi>[0] = {
      '/api/v1/subjects': { status: 500, body: {} },
    }
    stubApi(responses)
    renderAt('/subjects')

    expect(await screen.findByRole('alert')).toHaveTextContent('Impossible de charger les sujets.')
    responses['/api/v1/subjects'] = { body: TEST_SUBJECTS }
    await userEvent.click(screen.getByRole('button', { name: 'Réessayer' }))

    expect(await screen.findByRole('link', { name: /^Java/ })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
