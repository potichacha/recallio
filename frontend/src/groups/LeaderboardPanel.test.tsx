import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  fakeSession,
  renderAt,
  SIGNED_IN_API,
  stubApi,
  TEST_GROUP_SETTINGS,
  TEST_LEADERBOARD,
} from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Bob' })

const GROUP = {
  id: 'g1',
  name: 'Équipe backend',
  createdAt: '2026-11-01T10:00:00Z',
  myRole: 'MEMBER',
  settings: TEST_GROUP_SETTINGS,
  members: [],
}

const entry = (rank: number, displayName: string | null, overrides: object = {}) => ({
  rank,
  displayName,
  me: false,
  regularDays: 0,
  points: 0,
  cardsReviewed: 0,
  ...overrides,
})

function renderLeaderboard(leaderboard: object) {
  stubApi({
    ...SIGNED_IN_API,
    '/api/v1/groups/g1': { body: GROUP },
    '/api/v1/groups/g1/leaderboard': { body: { ...TEST_LEADERBOARD, ...leaderboard } },
  })
  renderAt('/groups/g1', signedIn())
  return screen.findByRole('region', { name: 'Classement de la semaine' })
}

describe('LeaderboardPanel', () => {
  it('ranks the members of the week, regularity first', async () => {
    const panel = await renderLeaderboard({
      entries: [
        entry(1, 'Alice', { regularDays: 3, points: 30 }),
        entry(2, 'Bob', { me: true, regularDays: 1, points: 500 }),
        entry(3, 'Carol'),
      ],
    })

    expect(await within(panel).findByText(/Du 26 octobre au 1 novembre/)).toHaveTextContent(
      'Un jour est régulier à partir de 10 cartes révisées.',
    )
    const rows = within(panel).getAllByRole('listitem')
    expect(rows.map((row) => row.textContent)).toEqual([
      '1Alice3 jours réguliers · 30 pts',
      '2Bob (vous)1 jour régulier · 500 pts',
      '3Carol0 jour régulier · 0 pts',
    ])
  })

  it('names only the user in an anonymous leaderboard', async () => {
    const panel = await renderLeaderboard({
      visibility: 'ANONYMOUS',
      entries: [entry(1, null), entry(2, 'Bob', { me: true })],
    })

    const rows = await within(panel).findAllByRole('listitem')
    expect(rows[0]).toHaveTextContent('Membre anonyme')
    expect(rows[1]).toHaveTextContent('Bob (vous)')
  })

  it('says when the owner disabled the leaderboard', async () => {
    const panel = await renderLeaderboard({ visibility: 'HIDDEN' })

    expect(
      await within(panel).findByText('Le créateur a désactivé le classement de ce groupe.'),
    ).toBeInTheDocument()
    expect(within(panel).queryByRole('listitem')).not.toBeInTheDocument()
  })
})
