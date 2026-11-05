import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { Duel } from '../api/ranked'
import { fakeSession, renderAt, sentRequests, SIGNED_IN_API, stubApi } from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

function duel(overrides: Partial<Duel> = {}): Duel {
  return {
    id: 'd1',
    subject: 'go',
    mode: 'ASYNC',
    status: 'FULL',
    questionCount: 5,
    expiresAt: '2026-11-07T10:00:00Z',
    me: { answered: 4, correct: 3, finished: false },
    opponent: { displayName: 'Bob', answered: 5, finished: true },
    result: null,
    ...overrides,
  }
}

describe('DuelPage', () => {
  it('serves the next question with its server timer, then shows the correction', async () => {
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      '/api/v1/ranked/duels/d1': { body: duel() },
      'POST /api/v1/ranked/duels/d1/next': {
        body: {
          finished: false,
          position: 4,
          total: 5,
          remainingMs: 30_000,
          question: {
            cardId: 'c5',
            type: 'TRUE_FALSE',
            prompt: 'Une goroutine coûte un thread système.',
            choices: ['Vrai', 'Faux'],
            multipleAnswers: false,
            blankCount: 0,
          },
        },
      },
      'POST /api/v1/ranked/duels/d1/answers': {
        body: {
          correct: true,
          late: false,
          correctChoices: [1],
          blanks: [],
          explanation: 'Quelques Ko de pile.',
          finished: true,
        },
      },
    })
    renderAt('/ranked/duels/d1', signedIn())

    expect(
      await screen.findByRole('heading', { name: 'Duel classé contre Bob' }),
    ).toBeInTheDocument()
    expect(await screen.findByText('Question 5 sur 5')).toBeInTheDocument()
    expect(screen.getByRole('timer', { name: 'Temps restant' })).toHaveTextContent('30 s')
    await userEvent.click(screen.getByRole('radio', { name: 'Faux' }))
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))

    expect(await screen.findByText('Juste !')).toBeInTheDocument()
    expect(screen.getByText('C’était la dernière question.')).toBeInTheDocument()
    const answer = sentRequests(fetchMock).find((request) => request.url.endsWith('/answers'))
    expect(await answer?.json()).toEqual({ position: 4, selectedChoices: [1] })
  })

  it('shows the result, the new rank and offers to share a promotion', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/ranked/duels/d1': {
        body: duel({
          status: 'FINISHED',
          me: { answered: 5, correct: 4, finished: true },
          result: {
            outcome: 'WIN',
            myCorrect: 4,
            opponentCorrect: 3,
            myTimeMs: 41_200,
            opponentTimeMs: 52_000,
            placing: false,
            rankBefore: { tier: 'SILVER', division: 1 },
            rankAfter: { tier: 'GOLD', division: 4 },
            promoted: true,
          },
        }),
      },
    })
    renderAt('/ranked/duels/d1', signedIn())

    expect(await screen.findByText('Victoire !')).toBeInTheDocument()
    expect(screen.getByText('4 / 5 contre 3 / 5 (41.2 s contre 52 s)')).toBeInTheDocument()
    expect(screen.getByText('Rang : Or IV')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Partager ma promotion' })).toBeInTheDocument()
  })

  it('waits for the opponent once the player is done', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/ranked/duels/d1': {
        body: duel({
          status: 'OPEN',
          me: { answered: 5, correct: 2, finished: true },
          opponent: null,
        }),
      },
    })
    renderAt('/ranked/duels/d1', signedIn())

    expect(await screen.findByText('Tu as fini : 2 / 5 bonnes réponses.')).toBeInTheDocument()
    expect(screen.getByText(/Un adversaire va rejoindre ce duel/)).toBeInTheDocument()
  })
})
