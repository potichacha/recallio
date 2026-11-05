import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, sentRequests, stubApi, TEST_SUBJECTS } from '../test/render'

const card = (overrides: Record<string, unknown>) => ({
  answer: null,
  choices: [],
  multipleAnswers: false,
  blankCount: 0,
  exercise: null,
  ...overrides,
})

const TRIAL = {
  questions: [
    {
      notion: 'hello-world',
      card: card({
        cardId: 'c1',
        type: 'TRUE_FALSE',
        prompt: '`main` prend des paramètres.',
        choices: ['Vrai', 'Faux'],
      }),
    },
    {
      notion: 'valeur-zero',
      card: card({
        cardId: 'c2',
        type: 'FILL_IN',
        prompt: '`var n int` vaut `___`',
        blankCount: 1,
      }),
    },
  ],
}

const correction = (overrides: Record<string, unknown>) => ({
  correctChoices: [],
  blanks: [],
  explanation: '',
  ...overrides,
})

describe('TrialPage', () => {
  it('asks a few questions corrected at once, then invites to create an account', async () => {
    const fetchMock = stubApi({
      '/api/v1/subjects': { body: TEST_SUBJECTS },
      '/api/v1/subjects/go/trial': { body: TRIAL },
      'POST /api/v1/subjects/go/trial/hello-world/checks': {
        body: correction({ cardId: 'c1', correct: false, correctChoices: [1] }),
      },
      'POST /api/v1/subjects/go/trial/valeur-zero/checks': {
        body: correction({
          cardId: 'c2',
          correct: true,
          blanks: [{ expected: '0', correct: true }],
        }),
      },
    })
    const session = fakeSession()
    renderAt('/try/go', session)

    expect(await screen.findByRole('heading', { name: 'Essai : Go' })).toBeInTheDocument()
    expect(screen.getByText('Question 1 sur 2')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('radio', { name: 'Vrai' }))
    await userEvent.click(screen.getByRole('button', { name: 'Vérifier' }))
    expect(await screen.findByText('Pas tout à fait.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Question suivante' }))

    expect(screen.getByText('Question 2 sur 2')).toBeInTheDocument()
    await userEvent.type(screen.getByRole('textbox', { name: 'Trou 1' }), '0')
    await userEvent.click(screen.getByRole('button', { name: 'Vérifier' }))
    expect(await screen.findByText('Juste !')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Question suivante' }))

    const summary = screen.getByRole('region', { name: 'Bilan de l’essai' })
    expect(summary).toHaveTextContent('1 / 2')
    await userEvent.click(within(summary).getByRole('button', { name: 'Créer mon compte' }))
    expect(session.login).toHaveBeenCalledWith('/welcome')
    const [first] = sentRequests(fetchMock).filter((request) => request.method === 'POST')
    expect(await first?.json()).toEqual({ cardId: 'c1', selectedChoices: [0] })
  })

  it('says when a subject has no trial yet', async () => {
    stubApi({
      '/api/v1/subjects': { body: TEST_SUBJECTS },
      '/api/v1/subjects/kafka/trial': {
        status: 404,
        body: { status: 404, title: 'Not Found', detail: 'Pas d’essai pour kafka' },
      },
    })
    renderAt('/try/kafka')

    expect(
      await screen.findByRole('heading', { name: 'Pas encore d’essai pour ce sujet' }),
    ).toBeInTheDocument()
  })
})
