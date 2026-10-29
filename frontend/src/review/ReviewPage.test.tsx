import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { QueuedCard, ReviewQueue } from '../api/review'
import { fakeSession, renderAt, sentRequests, stubApi } from '../test/render'

const QUEUE = '/api/v1/review-queue'
const REVIEWS = 'POST /api/v1/reviews'
const PROFILE = {
  '/api/v1/me': { body: { id: 'u1', displayName: 'Alice', timezone: 'Europe/Paris', roles: [] } },
}
const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const MINUTE = 60_000
const DAY = 24 * 60 * MINUTE

function queuedCard(overrides: Partial<QueuedCard> = {}): QueuedCard {
  const inMs = (delay: number) => new Date(Date.now() + delay).toISOString()
  return {
    cardId: 'card-1',
    type: 'FLASHCARD',
    themeSlug: 'java-fondamentaux',
    themeTitle: 'Java : fondamentaux',
    prompt: "Qu'est-ce que la JVM ?",
    answer: 'La machine virtuelle qui exécute le bytecode.',
    state: 'NEW',
    previews: [
      { rating: 'AGAIN', dueAt: inMs(MINUTE) },
      { rating: 'HARD', dueAt: inMs(6 * MINUTE) },
      { rating: 'GOOD', dueAt: inMs(10 * MINUTE) },
      { rating: 'EASY', dueAt: inMs(8 * DAY) },
    ],
    ...overrides,
  }
}

function queue(cards: QueuedCard[], overrides: Partial<ReviewQueue> = {}): ReviewQueue {
  return { cards, dueCount: 0, newCount: cards.length, nextDueAt: null, ...overrides }
}

const recorded = {
  body: { cardId: 'card-1', rating: 'GOOD', state: 'LEARNING', dueAt: '2026-10-29T10:10:00Z' },
}

describe('ReviewPage', () => {
  it('asks anonymous visitors to sign in', () => {
    renderAt('/review')

    expect(screen.getByRole('heading', { name: 'Connexion requise' })).toBeInTheDocument()
  })

  it('reveals the answer, shows the delay of each rating and records the chosen one', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: {
        body: queue([queuedCard(), queuedCard({ cardId: 'card-2', prompt: 'Taille d’un int ?' })]),
      },
      [REVIEWS]: { status: 201, ...recorded },
    })
    renderAt('/review', signedIn())

    expect(await screen.findByText("Qu'est-ce que la JVM ?")).toBeInTheDocument()
    expect(
      screen.queryByText('La machine virtuelle qui exécute le bytecode.'),
    ).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Voir la réponse' }))

    expect(screen.getByText('La machine virtuelle qui exécute le bytecode.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Bien' })).toHaveTextContent('10 min')
    expect(screen.getByRole('button', { name: 'Facile' })).toHaveTextContent('8 j')
    await userEvent.click(screen.getByRole('button', { name: 'Bien' }))

    expect(await screen.findByText('Taille d’un int ?')).toBeInTheDocument()
    expect(screen.getByText('1 carte révisée')).toBeInTheDocument()
    const post = sentRequests(fetchMock).find((request) => request.method === 'POST')
    const body = await post?.json()
    expect(body).toMatchObject({ cardId: 'card-1', rating: 'GOOD' })
    expect(body.requestId).toMatch(/^[0-9a-f-]{36}$/)
    expect(body.elapsedMs).toBeGreaterThanOrEqual(0)
  })

  it('sends the same request id again when retrying after an error', async () => {
    const responses = {
      ...PROFILE,
      [QUEUE]: { body: queue([queuedCard()]) },
      [REVIEWS]: {
        status: 422,
        body: {
          type: 'about:blank',
          title: 'Refusé',
          status: 422,
          detail: 'Erreur temporaire',
          code: 'x',
        },
      },
    }
    const fetchMock = stubApi(responses)
    renderAt('/review', signedIn())

    await userEvent.click(await screen.findByRole('button', { name: 'Voir la réponse' }))
    await userEvent.click(screen.getByRole('button', { name: 'Difficile' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Erreur temporaire')

    responses[REVIEWS] = {
      status: 201,
      ...recorded,
    } as unknown as (typeof responses)[typeof REVIEWS]
    await userEvent.click(screen.getByRole('button', { name: 'Réessayer' }))

    await screen.findByText('Série terminée, bravo !')
    const posts = sentRequests(fetchMock).filter((request) => request.method === 'POST')
    const [first, second] = await Promise.all(posts.map((request) => request.json()))
    expect(second.requestId).toBe(first.requestId)
    expect(second.rating).toBe('HARD')
  })

  it('offers to continue once the batch is finished', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: { body: queue([queuedCard()]) },
      [REVIEWS]: { status: 201, ...recorded },
    })
    renderAt('/review', signedIn())

    await userEvent.click(await screen.findByRole('button', { name: 'Voir la réponse' }))
    await userEvent.click(screen.getByRole('button', { name: 'Facile' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Continuer' }))

    await waitFor(() =>
      expect(sentRequests(fetchMock).filter((request) => request.url.includes(QUEUE))).toHaveLength(
        2,
      ),
    )
  })

  it('tells when everything is up to date, and when the next card is due', async () => {
    const nextDueAt = new Date(Date.now() + 10 * MINUTE).toISOString()
    stubApi({ ...PROFILE, [QUEUE]: { body: queue([], { nextDueAt }) } })
    renderAt('/review', signedIn())

    expect(await screen.findByText('Tout est à jour pour le moment.')).toBeInTheDocument()
    expect(screen.getByText('Prochaine carte à revoir dans 10 min.')).toBeInTheDocument()
  })

  it('reviews a single theme when one is chosen', async () => {
    const fetchMock = stubApi({ ...PROFILE, [QUEUE]: { body: queue([]) } })
    renderAt('/review?theme=java-fondamentaux', signedIn())

    await screen.findByText('Tout est à jour pour le moment.')
    const request = sentRequests(fetchMock).find((sent) => sent.url.includes(QUEUE))
    expect(new URL(request?.url ?? '').searchParams.get('theme')).toBe('java-fondamentaux')
  })

  it('lets the candidate write an answer to a scenario before revealing the model answer', async () => {
    stubApi({ ...PROFILE, [QUEUE]: { body: queue([queuedCard({ type: 'OPEN_SCENARIO' })]) } })
    renderAt('/review', signedIn())

    const draft = await screen.findByRole('textbox', { name: 'Votre réponse (facultatif)' })
    await userEvent.type(draft, 'Je profile avec JFR.')
    await userEvent.click(screen.getByRole('button', { name: 'Voir la réponse' }))

    expect(draft).toHaveValue('Je profile avec JFR.')
    expect(screen.getByText('La machine virtuelle qui exécute le bytecode.')).toBeInTheDocument()
  })
})
