import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, sentRequests, SIGNED_IN_API, stubApi } from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Bob' })

const CHALLENGE = '/api/v1/challenges/c1'

function challenge(summary: object = {}, rest: object = {}) {
  return {
    summary: {
      id: 'c1',
      title: 'Défi du lundi',
      cardCount: 2,
      secondsPerCard: 30,
      endsAt: '2026-11-09T10:00:00Z',
      open: true,
      myStatus: 'NOT_STARTED',
      myScore: null,
      ...summary,
    },
    groupId: 'g1',
    groupName: 'Équipe backend',
    visibility: 'NAMED',
    standings: [],
    ...rest,
  }
}

const card = (position: number, remainingMs = 30_000) => ({
  position,
  subjectTitle: 'Go',
  themeTitle: 'Fondamentaux',
  prompt: `Question ${position + 1} ?`,
  choices: ['Oui', 'Non'],
  multipleAnswers: false,
  remainingMs,
})

const step = (cardOrNull: object | null, score = 0) => ({
  card: cardOrNull,
  cardCount: 2,
  score,
  finished: cardOrNull === null,
})

describe('ChallengePage', () => {
  it('plays the challenge card after card, then shows the results', async () => {
    const api = {
      ...SIGNED_IN_API,
      [CHALLENGE]: { body: challenge() } as { body: unknown },
      [`POST ${CHALLENGE}/next`]: { body: step(card(0)) } as { body: unknown },
      [`POST ${CHALLENGE}/answers`]: {
        body: {
          correct: true,
          timedOut: false,
          correctChoices: [0],
          explanation: 'Évidemment.',
          finished: false,
        },
      } as { body: unknown },
    }
    const fetchMock = stubApi(api)
    renderAt('/challenges/c1', signedIn())

    expect(await screen.findByRole('heading', { name: 'Défi du lundi' })).toBeInTheDocument()
    expect(screen.getByText(/2 QCM, 30 secondes par carte/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Commencer' }))

    expect(await screen.findByText('Carte 1 / 2')).toBeInTheDocument()
    expect(screen.getByRole('timer', { name: 'Temps restant' })).toHaveTextContent('30 s')
    await userEvent.click(screen.getByRole('radio', { name: /Oui/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Bonne réponse !')
    const answer = sentRequests(fetchMock).find((request) => request.url.endsWith('/answers'))
    expect(await answer?.json()).toEqual({ position: 0, selectedChoices: [0] })

    // La partie se termine : le serveur le dit, et le défi est rechargé avec le classement.
    api[`POST ${CHALLENGE}/next`] = { body: step(null, 1) }
    api[CHALLENGE] = {
      body: challenge(
        { myStatus: 'FINISHED', myScore: 1 },
        { standings: [{ rank: 1, displayName: 'Bob', me: true, score: 1, timeMs: 4200 }] },
      ),
    }
    await userEvent.click(screen.getByRole('button', { name: 'Carte suivante' }))

    expect(await screen.findByText('Ton score : 1 / 2')).toBeInTheDocument()
    expect(screen.getByRole('listitem')).toHaveTextContent('1Bob (toi)1 / 2 · 4 s')
    expect(screen.queryByRole('button', { name: 'Commencer' })).not.toBeInTheDocument()
  })

  it('tells when the time is up, and moves on', async () => {
    stubApi({
      ...SIGNED_IN_API,
      [CHALLENGE]: { body: challenge({ myStatus: 'IN_PROGRESS' }) },
      [`POST ${CHALLENGE}/next`]: { body: step(card(1, 0)) },
    })
    renderAt('/challenges/c1', signedIn())

    await userEvent.click(await screen.findByRole('button', { name: 'Reprendre' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Temps écoulé')
    expect(screen.queryByRole('button', { name: 'Valider' })).not.toBeInTheDocument()
    expect(screen.getByRole('radio', { name: /Oui/ })).toBeDisabled()
  })

  it('hides the names of the others in an anonymous leaderboard', async () => {
    stubApi({
      ...SIGNED_IN_API,
      [CHALLENGE]: {
        body: challenge(
          { open: false },
          {
            visibility: 'ANONYMOUS',
            standings: [{ rank: 1, displayName: null, me: false, score: 2, timeMs: 9000 }],
          },
        ),
      },
    })
    renderAt('/challenges/c1', signedIn())

    expect(
      await screen.findByText('Ce défi est terminé : on ne peut plus y jouer.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('listitem')).toHaveTextContent('Membre anonyme')
    expect(screen.queryByRole('button', { name: 'Commencer' })).not.toBeInTheDocument()
  })

  it('shows a not-found message for a challenge of another group', async () => {
    stubApi({
      ...SIGNED_IN_API,
      [CHALLENGE]: { status: 404, body: { code: 'group.challenge-not-found' } },
    })
    renderAt('/challenges/c1', signedIn())

    expect(await screen.findByRole('heading', { name: 'Défi introuvable' })).toBeInTheDocument()
  })
})
