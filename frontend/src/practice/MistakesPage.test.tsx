import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  fakeSession,
  renderAt,
  sentRequests,
  SIGNED_IN_API,
  stubApi,
  TEST_SUBJECTS,
} from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const MISTAKES = {
  cards: [
    {
      cardId: 'c1',
      type: 'TRUE_FALSE',
      prompt: 'Un channel fermé se lit encore.',
      answer: null,
      choices: ['Vrai', 'Faux'],
      multipleAnswers: false,
      blankCount: 0,
      exercise: null,
      notionSlug: 'channels-fermes',
      notionTitle: 'Les channels fermés',
    },
    {
      cardId: 'c2',
      type: 'TRUE_FALSE',
      prompt: '`defer` s’exécute à la sortie de la fonction.',
      answer: null,
      choices: ['Vrai', 'Faux'],
      multipleAnswers: false,
      blankCount: 0,
      exercise: null,
      notionSlug: 'defer',
      notionTitle: 'defer',
    },
  ],
}

const API = { ...SIGNED_IN_API, '/api/v1/subjects': { body: TEST_SUBJECTS } }
const CHECKS = 'POST /api/v1/me/mistakes/go/checks'

describe('MistakesPage (ADR 0057)', () => {
  it('goes through the cards failed this week, the server correcting without recording', async () => {
    const responses: Parameters<typeof stubApi>[0] = {
      ...API,
      '/api/v1/me/mistakes/go': { body: MISTAKES },
      [CHECKS]: {
        body: { cardId: 'c1', correct: false, correctChoices: [0], blanks: [], explanation: 'Si.' },
      },
    }
    const fetchMock = stubApi(responses)
    renderAt('/subjects/go/mistakes', signedIn())

    expect(
      await screen.findByRole('heading', { name: 'Mes erreurs de la semaine : Go' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Carte 1 sur 2 · Les channels fermés')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('radio', { name: /Faux/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Vérifier' }))

    // Encore fausse : la leçon de sa notion est à un clic.
    expect(await screen.findByRole('link', { name: /Revoir la leçon/ })).toHaveAttribute(
      'href',
      '/subjects/go/course/channels-fermes',
    )
    const check = sentRequests(fetchMock).find((request) => request.method === 'POST')
    expect(await check?.json()).toEqual({ cardId: 'c1', selectedChoices: [1] })

    responses[CHECKS] = {
      body: { cardId: 'c2', correct: true, correctChoices: [0], blanks: [], explanation: 'Oui.' },
    }
    await userEvent.click(screen.getByRole('button', { name: 'Carte suivante' }))
    expect(screen.getByText('Carte 2 sur 2 · defer')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('radio', { name: /Vrai/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Vérifier' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Carte suivante' }))

    expect(screen.getByRole('status')).toHaveTextContent('Erreurs revues !')
    expect(screen.getByText(/1 \/ 2 juste/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Revenir aux révisions' })).toHaveAttribute(
      'href',
      '/review?subject=go',
    )
  })

  it('says when there is nothing to see again this week', async () => {
    stubApi({ ...API, '/api/v1/me/mistakes/go': { body: { cards: [] } } })
    renderAt('/subjects/go/mistakes', signedIn())

    expect(await screen.findByText('Aucune erreur à revoir cette semaine.')).toBeInTheDocument()
  })

  it('asks a visitor to sign in', async () => {
    renderAt('/subjects/go/mistakes')

    expect(await screen.findByRole('heading', { name: 'Connexion requise' })).toBeInTheDocument()
  })
})
