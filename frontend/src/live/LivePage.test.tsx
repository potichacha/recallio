import { act, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { setAccessTokenProvider } from '../api/client'
import {
  fakeSession,
  renderAt,
  sentRequests,
  SIGNED_IN_API,
  stubApi,
  TEST_SUBJECTS,
} from '../test/render'

/** Un WebSocket factice : le test joue le rôle du serveur (messages, fermeture). */
class FakeWebSocket {
  static instances: FakeWebSocket[] = []
  onmessage?: (event: { data: string }) => void
  onclose?: (event: { code: number; reason: string }) => void
  closed = false

  readonly url: string
  readonly protocols: string[]

  constructor(url: string, protocols: string[]) {
    this.url = url
    this.protocols = protocols
    FakeWebSocket.instances.push(this)
  }

  close() {
    this.closed = true
  }

  receive(event: object) {
    act(() => this.onmessage?.({ data: JSON.stringify(event) }))
  }

  serverClose(code: number, reason = '') {
    act(() => this.onclose?.({ code, reason }))
  }
}

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const player = (userId: string, displayName: string, host = false) => ({
  userId,
  displayName,
  host,
})

const game = (players: object[], status = 'LOBBY', round: object | null = null) => ({
  type: 'game',
  game: {
    id: 'game-1',
    groupId: 'g1',
    subject: 'java',
    hostId: 'u1',
    createdAt: '2026-11-02T18:00:00Z',
    status,
    players,
    round,
  },
})

const round = (reveal: object | null = null, answeredCount = 0) => ({
  index: 0,
  total: 3,
  prompt: 'Où vivent les objets ?',
  choices: ['Dans la pile', 'Dans le tas'],
  multipleAnswers: false,
  remainingMs: 15_000,
  answeredCount,
  reveal,
})

/** La dernière connexion ouverte, une fois le jeton obtenu (asynchrone). */
async function socket() {
  await waitFor(() => expect(FakeWebSocket.instances.length).toBeGreaterThan(0))
  return FakeWebSocket.instances.at(-1)!
}

describe('LivePage', () => {
  beforeEach(() => {
    FakeWebSocket.instances = []
    vi.stubGlobal('WebSocket', FakeWebSocket)
    stubApi({ ...SIGNED_IN_API, '/api/v1/subjects': { body: TEST_SUBJECTS } })
  })

  afterEach(() => setAccessTokenProvider(async () => undefined))

  it('shows the players of the lobby, updated live', async () => {
    renderAt('/live/game-1', signedIn())

    const connection = await socket()
    expect(connection.url).toMatch(/^ws:\/\/.+\/api\/v1\/live\/game-1$/)
    connection.receive(game([player('u1', 'Alice', true)]))

    expect(await screen.findByRole('heading', { name: 'Partie live : Java' })).toBeInTheDocument()
    expect(screen.getByText(/Vous êtes l’hôte/)).toBeInTheDocument()
    const players = screen.getByRole('region', { name: 'Joueurs (1)' })
    expect(within(players).getByRole('listitem')).toHaveTextContent('Alice(vous)Hôte')

    connection.receive(game([player('u1', 'Alice', true), player('u2', 'Bob')]))
    expect(await screen.findByRole('region', { name: 'Joueurs (2)' })).toHaveTextContent('Bob')
  })

  it('lets the host start, then plays a question up to its correction', async () => {
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      '/api/v1/subjects': { body: TEST_SUBJECTS },
      'POST /api/v1/live-games/game-1/start': { body: game([]).game },
      'POST /api/v1/live-games/game-1/answers': { body: game([]).game },
    })
    renderAt('/live/game-1', signedIn())
    const connection = await socket()
    const players = [player('u1', 'Alice', true), player('u2', 'Bob')]
    connection.receive(game(players))

    await userEvent.click(await screen.findByRole('button', { name: 'Démarrer la partie' }))
    expect(sentRequests(fetchMock).some((request) => request.url.endsWith('/start'))).toBe(true)

    // Le nouvel état arrive par le WebSocket, à tous les joueurs en même temps.
    connection.receive(game(players, 'QUESTION', round()))
    expect(await screen.findByText('Question 1 / 3')).toBeInTheDocument()
    expect(screen.getByRole('timer', { name: 'Temps restant' })).toHaveTextContent('15 s')
    await userEvent.click(screen.getByRole('radio', { name: /Dans la pile/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Réponse envoyée.')
    const answer = sentRequests(fetchMock).find((request) => request.url.endsWith('/answers'))
    expect(await answer?.json()).toEqual({ question: 0, selectedChoices: [0] })

    connection.receive(
      game(
        [player('u1', 'Alice', true), { ...player('u2', 'Bob'), answered: true }],
        'REVEAL',
        round({ correctChoices: [1], explanation: 'Dans le **tas**.', correctCount: 1 }, 2),
      ),
    )
    expect(await screen.findByText('1 joueur sur 2 a trouvé.')).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: /Dans le tas/ }).closest('label')).toHaveTextContent(
      'Bonne réponse, non cochée',
    )
    expect(screen.getByRole('timer', { name: 'Avant la suite' })).toBeInTheDocument()

    connection.receive(
      game(players, 'FINISHED', round({ correctChoices: [1], explanation: '', correctCount: 1 })),
    )
    expect(await screen.findByText('Partie terminée : merci d’avoir joué !')).toBeInTheDocument()
  })

  it('waits for the host when the player is not the host', async () => {
    renderAt('/live/game-1', signedIn())

    ;(await socket()).receive({
      ...game([player('u2', 'Bob', true)]),
      game: { ...game([]).game, hostId: 'u2' },
    })

    expect(await screen.findByText('En attente du lancement par l’hôte.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Démarrer la partie' })).not.toBeInTheDocument()
  })

  it('carries the access token in the subprotocol, the browser cannot send headers', async () => {
    setAccessTokenProvider(async () => 'jeton')
    renderAt('/live/game-1', signedIn())

    const connection = await socket()
    expect(connection.protocols).toEqual([
      'bearer-token-carrier',
      encodeURIComponent('quarkus-http-upgrade#Authorization#Bearer jeton'),
    ])
  })

  it('explains a game that does not exist, or no longer', async () => {
    renderAt('/live/game-1', signedIn())

    ;(await socket()).serverClose(4404, 'Aucune partie')

    expect(await screen.findByRole('heading', { name: 'Partie introuvable' })).toBeInTheDocument()
  })

  it('offers to reconnect after a lost connection, with a new socket', async () => {
    renderAt('/live/game-1', signedIn())
    const lost = await socket()
    lost.serverClose(1006)

    await userEvent.click(await screen.findByRole('button', { name: 'Se reconnecter' }))

    await waitFor(() => expect(FakeWebSocket.instances).toHaveLength(2))
    expect(await screen.findByText('Connexion à la partie…')).toBeInTheDocument()
  })
})
