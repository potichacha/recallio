import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { Duel, RankedOverview } from '../api/ranked'
import { fakeSession, renderAt, SIGNED_IN_API, stubApi } from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const OVERVIEW: RankedOverview = {
  season: '2026-T4',
  seasonEndsOn: '2027-01-01',
  subjects: [
    {
      subject: 'java',
      title: 'Java',
      placing: true,
      seasonDuels: 2,
      placementDuels: 5,
      rank: null,
      wins: 1,
      losses: 1,
      draws: 0,
    },
    {
      subject: 'go',
      title: 'Go',
      placing: false,
      seasonDuels: 9,
      placementDuels: 5,
      rank: { tier: 'GOLD', division: 2 },
      wins: 6,
      losses: 3,
      draws: 0,
    },
  ],
  badges: [{ subject: 'go', title: 'Go', season: '2026-T3', tier: 'SILVER' }],
}

export function openDuel(overrides: Partial<Duel> = {}): Duel {
  return {
    id: 'd1',
    subject: 'go',
    mode: 'ASYNC',
    status: 'OPEN',
    questionCount: 5,
    expiresAt: '2026-11-07T10:00:00Z',
    me: { answered: 0, correct: 0, finished: false },
    opponent: null,
    result: null,
    ...overrides,
  }
}

describe('RankedPage', () => {
  it('shows a rank per subject, hidden during the placement, and the season badges', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/me/ranked': { body: OVERVIEW },
      '/api/v1/me/ranked/duels': {
        body: [
          {
            id: 'd0',
            subject: 'go',
            mode: 'LIVE',
            status: 'FINISHED',
            expiresAt: '2026-11-04T10:05:00Z',
            opponentName: 'Bob',
            outcome: 'WIN',
            myTurnDone: true,
          },
        ],
      },
    })
    renderAt('/ranked', signedIn())

    expect(await screen.findByRole('heading', { name: 'Classé' })).toBeInTheDocument()
    const java = screen.getByRole('heading', { name: 'Java' }).closest('li') as HTMLElement
    expect(java).toHaveTextContent('Placement : 2 / 5')
    const go = screen.getByRole('heading', { name: 'Go' }).closest('li') as HTMLElement
    expect(go).toHaveTextContent('Or II')
    expect(go).toHaveTextContent('6 victoires, 3 défaites, 0 nul cette saison')
    expect(screen.getByRole('region', { name: 'Mes insignes de saison' })).toHaveTextContent(
      'Go · 2026-T3 · Argent',
    )
    const recent = await screen.findByRole('region', { name: 'Mes derniers duels' })
    expect(within(recent).getByRole('link', { name: 'go · direct contre Bob' })).toHaveAttribute(
      'href',
      '/ranked/duels/d0',
    )
    expect(recent).toHaveTextContent('Gagné')
  })

  it('finds a duel and opens it', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/me/ranked': { body: OVERVIEW },
      '/api/v1/me/ranked/duels': { body: [] },
      'POST /api/v1/ranked/duels': { body: openDuel() },
      '/api/v1/ranked/duels/d1': { body: openDuel() },
      'POST /api/v1/ranked/duels/d1/next': {
        body: { finished: false, position: 0, total: 5, question: null, remainingMs: 30_000 },
      },
    })
    const router = renderAt('/ranked', signedIn())

    const go = (await screen.findByRole('heading', { name: 'Go' })).closest('li') as HTMLElement
    await userEvent.click(within(go).getByRole('button', { name: 'Duel' }))

    expect(await screen.findByRole('heading', { name: 'Duel classé' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/ranked/duels/d1')
  })
})
