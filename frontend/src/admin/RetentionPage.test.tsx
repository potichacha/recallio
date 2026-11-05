import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, SIGNED_IN_API, stubApi, TEST_PROFILE } from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const RETENTION = {
  players: 12,
  days: [
    { day: 1, eligible: 10, retained: 4, percent: 40 },
    { day: 7, eligible: 6, retained: 2, percent: 33 },
    { day: 30, eligible: 0, retained: 0, percent: 0 },
  ],
}

describe('RetentionPage', () => {
  it('shows an admin the share of players back 1, 7 and 30 days after their first day', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/me': { body: { ...TEST_PROFILE, roles: ['admin'] } },
      '/api/v1/admin/retention': { body: RETENTION },
    })
    renderAt('/admin/retention', signedIn())

    expect(await screen.findByRole('heading', { name: 'Rétention' })).toBeInTheDocument()
    expect(screen.getByText(/12 joueurs ont révisé au moins une fois/)).toBeInTheDocument()
    expect(screen.getByText('J1').nextElementSibling).toHaveTextContent('40 %')
    expect(screen.getByText('J7').parentElement).toHaveTextContent('2 sur 6 joueurs')
    // Personne n'a encore trente jours d'ancienneté : pas de pourcentage.
    expect(screen.getByText('J30').nextElementSibling).toHaveTextContent('–')
  })

  it('is reserved to the admins', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/admin/retention': {
        status: 403,
        body: { status: 403, title: 'Forbidden', detail: 'Réservé aux admins' },
      },
    })
    renderAt('/admin/retention', signedIn())

    expect(
      await screen.findByText('Cette page est réservée aux administrateurs.'),
    ).toBeInTheDocument()
  })
})
