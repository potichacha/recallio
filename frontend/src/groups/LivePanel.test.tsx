import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import {
  fakeSession,
  renderAt,
  sentRequests,
  SIGNED_IN_API,
  stubApi,
  TEST_GROUP_SETTINGS,
  TEST_LEADERBOARD,
  TEST_SUBJECTS,
} from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Bob' })

const GROUP = {
  id: 'g1',
  name: 'Équipe backend',
  createdAt: '2026-11-01T10:00:00Z',
  myRole: 'MEMBER',
  settings: { ...TEST_GROUP_SETTINGS, subjects: ['java', 'go'] },
  members: [],
}

const LIVE_GAME = {
  id: 'game-1',
  groupId: 'g1',
  subject: 'go',
  hostId: 'u2',
  createdAt: '2026-11-02T18:00:00Z',
  status: 'LOBBY',
  players: [{ userId: 'u2', displayName: 'Alice', host: true }],
}

function stubGroup(extra: object) {
  return stubApi({
    ...SIGNED_IN_API,
    '/api/v1/groups/g1': { body: GROUP },
    '/api/v1/groups/g1/leaderboard': { body: TEST_LEADERBOARD },
    '/api/v1/groups/g1/challenges': { body: [] },
    '/api/v1/subjects': { body: TEST_SUBJECTS },
    '/api/v1/groups/g1/live-games/results': { body: [] },
    ...extra,
  })
}

describe('LivePanel', () => {
  it('invites to join the open game of the group', async () => {
    stubGroup({ '/api/v1/groups/g1/live-games': { body: [LIVE_GAME] } })
    renderAt('/groups/g1', signedIn())

    const panel = await screen.findByRole('region', { name: 'Partie live' })
    expect(await within(panel).findByText(/Une partie sur/)).toHaveTextContent(
      'Une partie sur Go est ouverte (1 joueur).',
    )
    expect(within(panel).getByRole('link', { name: 'Rejoindre la partie' })).toHaveAttribute(
      'href',
      '/live/game-1',
    )
  })

  it('shows the podium of the last games of the group', async () => {
    const standing = (rank: number, userId: string, displayName: string, score: number) => ({
      rank,
      userId,
      displayName,
      score,
      correctCount: 1,
      lastPoints: 0,
    })
    stubGroup({
      '/api/v1/groups/g1/live-games': { body: [] },
      '/api/v1/groups/g1/live-games/results': {
        body: [
          {
            gameId: 'old',
            subject: 'go',
            startedAt: '2026-11-02T18:00:00Z',
            finishedAt: '2026-11-02T18:05:00Z',
            questionCount: 3,
            standings: [standing(1, 'u2', 'Alice', 2400), standing(2, 'u1', 'Bob', 900)],
          },
        ],
      },
    })
    renderAt('/groups/g1', signedIn())

    const panel = await screen.findByRole('region', { name: 'Partie live' })
    expect(await within(panel).findByText('Dernières parties')).toBeInTheDocument()
    expect(panel).toHaveTextContent('1. Alice (2400 pts) · 2. Bob (900 pts)')
  })

  it('opens a game on a subject of the group, then goes to its lobby', async () => {
    vi.stubGlobal(
      'WebSocket',
      class {
        close() {}
      },
    )
    // Une fois la partie ouverte, le serveur la liste : recharger la liste ferait disparaître le formulaire avant que
    // la navigation vers la salle d'attente ait eu lieu (bug corrigé).
    let opened = false
    const fetchMock = stubGroup({
      '/api/v1/groups/g1/live-games': {
        get body() {
          return opened ? [LIVE_GAME] : []
        },
      },
      'POST /api/v1/groups/g1/live-games': {
        status: 201,
        get body() {
          opened = true
          return LIVE_GAME
        },
      },
    })
    const router = renderAt('/groups/g1', signedIn())

    const form = await screen.findByRole('form', { name: 'Lancer une partie live' })
    await userEvent.selectOptions(within(form).getByLabelText('Sujet'), 'go')
    await userEvent.click(within(form).getByRole('button', { name: 'Lancer une partie live' }))

    expect(await screen.findByText('Connexion à la partie…')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/live/game-1')
    const post = sentRequests(fetchMock).find((request) => request.method === 'POST')
    expect(await post?.json()).toEqual({ subject: 'go' })
  })
})
