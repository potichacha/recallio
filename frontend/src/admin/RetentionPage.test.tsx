import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, SIGNED_IN_API, stubApi, TEST_PROFILE } from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const ADMIN_API = {
  ...SIGNED_IN_API,
  '/api/v1/me': { body: { ...TEST_PROFILE, roles: ['admin'] } },
}

const RETENTION = {
  players: 12,
  days: [
    { day: 1, eligible: 10, retained: 4, percent: 40 },
    { day: 7, eligible: 6, retained: 2, percent: 33 },
    { day: 30, eligible: 0, retained: 0, percent: 0 },
  ],
}

const steps = (visits: number, trials: number, accounts: number, firstReviews: number) => {
  const percent = (count: number, previous: number) =>
    previous === 0 ? null : Math.floor((count * 100) / previous)
  return [
    { step: 'VISIT', count: visits, percent: null },
    { step: 'TRIAL', count: trials, percent: percent(trials, visits) },
    { step: 'ACCOUNT', count: accounts, percent: percent(accounts, trials) },
    { step: 'FIRST_REVIEW', count: firstReviews, percent: percent(firstReviews, accounts) },
  ]
}

/** Huit semaines, la semaine en cours d'abord : les deux premières ont vu passer des visiteurs. */
const week = (weekStart: string, weekEnd: string, counts = steps(0, 0, 0, 0)) => ({
  weekStart,
  weekEnd,
  steps: counts,
})
const QUIET_WEEKS = [
  week('2026-11-02', '2026-11-08'),
  week('2026-10-26', '2026-11-01'),
  week('2026-10-19', '2026-10-25'),
  week('2026-10-12', '2026-10-18'),
  week('2026-10-05', '2026-10-11'),
  week('2026-09-28', '2026-10-04'),
]
const FUNNEL = {
  weeks: [
    week('2026-11-16', '2026-11-22', steps(40, 10, 3, 2)),
    // Deux essais, mais aucune visite comptée cette semaine-là : pas de taux.
    week('2026-11-09', '2026-11-15', steps(0, 2, 0, 0)),
    ...QUIET_WEEKS,
  ],
}

describe('RetentionPage', () => {
  it('shows an admin the share of players back 1, 7 and 30 days after their first day', async () => {
    stubApi({
      ...ADMIN_API,
      '/api/v1/admin/retention': { body: RETENTION },
      '/api/v1/admin/funnel': { body: FUNNEL },
    })
    renderAt('/admin/retention', signedIn())

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Rétention et parcours' }),
    ).toBeInTheDocument()
    expect(await screen.findByText(/12 joueurs ont révisé au moins une fois/)).toBeInTheDocument()
    expect(screen.getByText('J1').nextElementSibling).toHaveTextContent('40 %')
    expect(screen.getByText('J7').parentElement).toHaveTextContent('2 sur 6 joueurs')
    // Personne n'a encore trente jours d'ancienneté : pas de pourcentage.
    expect(screen.getByText('J30').nextElementSibling).toHaveTextContent('–')
  })

  it('shows the journey of the visitors week by week, with the rate from one step to the next', async () => {
    stubApi({
      ...ADMIN_API,
      '/api/v1/admin/retention': { body: RETENTION },
      '/api/v1/admin/funnel': { body: FUNNEL },
    })
    renderAt('/admin/retention', signedIn())

    const table = await screen.findByRole('table', { name: 'Parcours des visiteurs' })
    expect(
      within(table)
        .getAllByRole('columnheader')
        .map((header) => header.textContent),
    ).toEqual(['Semaine du', 'Visites', 'Essais', 'Comptes créés', 'Premières révisions'])
    const rows = within(table).getAllByRole('row')
    expect(rows).toHaveLength(1 + 8)

    const current = within(rows[1])
    expect(current.getByRole('rowheader')).toHaveTextContent('16 nov.')
    expect(current.getAllByRole('cell').map((cell) => cell.textContent)).toEqual([
      '40',
      '10 soit 25 %',
      '3 soit 30 %',
      '2 soit 66 %',
    ])
    // Aucune visite comptée la semaine précédente : le taux des essais n'existe pas.
    expect(
      within(rows[2])
        .getAllByRole('cell')
        .map((cell) => cell.textContent),
    ).toEqual(['0', '2 –', '0 soit 0 %', '0 –'])
  })

  it('says when nobody has gone through the journey yet', async () => {
    stubApi({
      ...ADMIN_API,
      '/api/v1/admin/retention': { body: RETENTION },
      '/api/v1/admin/funnel': {
        body: { weeks: [week('2026-11-16', '2026-11-22'), ...QUIET_WEEKS] },
      },
    })
    renderAt('/admin/retention', signedIn())

    expect(await screen.findByText('Personne n’est encore passé par là.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('offers to retry when the journey fails to load, and keeps the retention', async () => {
    const fetchMock = stubApi({
      ...ADMIN_API,
      '/api/v1/admin/retention': { body: RETENTION },
      '/api/v1/admin/funnel': {
        status: 500,
        body: { status: 500, title: 'Internal Server Error' },
      },
    })
    renderAt('/admin/retention', signedIn())

    const failure = await screen.findByRole('alert')
    expect(failure).toHaveTextContent('Impossible de charger le parcours.')
    expect(await screen.findByText(/12 joueurs ont révisé au moins une fois/)).toBeInTheDocument()

    await userEvent.click(within(failure).getByRole('button', { name: 'Réessayer' }))
    await waitFor(() =>
      expect(
        fetchMock.mock.calls.filter(([request]) => request.url.endsWith('/api/v1/admin/funnel')),
      ).toHaveLength(2),
    )
  })

  it('is reserved to the admins', async () => {
    const forbidden = {
      status: 403,
      body: { status: 403, title: 'Forbidden', detail: 'Réservé aux admins' },
    }
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/admin/retention': forbidden,
      '/api/v1/admin/funnel': forbidden,
    })
    renderAt('/admin/retention', signedIn())

    expect(
      await screen.findByText('Cette page est réservée aux administrateurs.'),
    ).toBeInTheDocument()
  })
})
