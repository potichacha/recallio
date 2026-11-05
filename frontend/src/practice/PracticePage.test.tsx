import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, sentRequests, SIGNED_IN_API, stubApi } from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const PRACTICE = {
  notionTitle: 'Le premier programme',
  cards: [
    {
      cardId: 'c1',
      type: 'TRUE_FALSE',
      prompt: '`main` prend des paramètres.',
      answer: null,
      choices: ['Vrai', 'Faux'],
      multipleAnswers: false,
      blankCount: 0,
      exercise: null,
    },
    {
      cardId: 'c2',
      type: 'FLASHCARD',
      prompt: 'Où commence un programme Go ?',
      answer: 'Dans `func main`, du package `main`.',
      choices: [],
      multipleAnswers: false,
      blankCount: 0,
      exercise: null,
    },
    {
      cardId: 'c3',
      type: 'FILL_IN',
      prompt: '`package ___`',
      answer: null,
      choices: [],
      multipleAnswers: false,
      blankCount: 1,
      exercise: null,
    },
  ],
}

const CHECKS = 'POST /api/v1/me/practice/go/hello-world/checks'

describe('PracticePage', () => {
  it('goes through every card of the notion, the server correcting what it can', async () => {
    const responses: Parameters<typeof stubApi>[0] = {
      ...SIGNED_IN_API,
      '/api/v1/me/practice/go/hello-world': { body: PRACTICE },
      [CHECKS]: {
        body: {
          cardId: 'c1',
          correct: false,
          correctChoices: [1],
          blanks: [],
          explanation: 'Non : `main` ne prend rien.',
        },
      },
    }
    const fetchMock = stubApi(responses)
    renderAt('/subjects/go/practice/hello-world?from=tour', signedIn())

    expect(
      await screen.findByRole('heading', { name: 'S’entraîner : Le premier programme' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Carte 1 sur 3')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('radio', { name: 'Vrai' }))
    await userEvent.click(screen.getByRole('button', { name: 'Vérifier' }))

    expect(await screen.findByText('Pas tout à fait.')).toBeInTheDocument()
    expect(screen.getByText('Bonne réponse, non cochée')).toBeInTheDocument()
    const sent = sentRequests(fetchMock).find((request) => request.url.endsWith('/checks'))
    expect(await sent?.json()).toEqual({ cardId: 'c1', selectedChoices: [0] })

    await userEvent.click(screen.getByRole('button', { name: 'Carte suivante' }))
    expect(screen.getByText('Carte 2 sur 3')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Voir la réponse' }))
    expect(screen.getByRole('region', { name: 'Réponse' })).toHaveTextContent(
      'Dans func main, du package main.',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Carte suivante' }))

    responses[CHECKS] = {
      body: {
        cardId: 'c3',
        correct: true,
        correctChoices: [],
        blanks: [{ expected: 'main', correct: true }],
        explanation: 'Le package `main`.',
      },
    }
    await userEvent.type(screen.getByRole('textbox', { name: 'Trou 1' }), 'main')
    await userEvent.click(screen.getByRole('button', { name: 'Vérifier' }))
    expect(await screen.findByText('Juste !')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Carte suivante' }))

    expect(screen.getByRole('status')).toHaveTextContent('Entraînement terminé !')
    expect(
      screen.getByText('1 / 2 juste parmi les cartes corrigées par le serveur.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Revenir au Tour' })).toHaveAttribute(
      'href',
      '/subjects/go/tour/hello-world',
    )

    await userEvent.click(screen.getByRole('button', { name: 'Recommencer' }))
    expect(screen.getByText('Carte 1 sur 3')).toBeInTheDocument()
    expect(within(screen.getByRole('group')).getByRole('radio', { name: 'Vrai' })).not.toBeChecked()
  })

  it('starts at the card of the link, like an exercise at the end of a chapter', async () => {
    stubApi({ ...SIGNED_IN_API, '/api/v1/me/practice/go/hello-world': { body: PRACTICE } })
    renderAt('/subjects/go/practice/hello-world?from=tour&card=c3', signedIn())

    expect(await screen.findByText('Carte 3 sur 3')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Trou 1' })).toBeInTheDocument()
  })

  it('links back to the learning path when it does not come from the Tour', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/me/practice/go/hello-world': { body: { ...PRACTICE, cards: [] } },
    })
    renderAt('/subjects/go/practice/hello-world', signedIn())

    expect(await screen.findByText('Cette notion n’a pas encore de cartes.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Voir mon parcours' })).toHaveAttribute(
      'href',
      '/subjects/go/path',
    )
  })

  it('shows a not-found page for an unknown notion', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/me/practice/go/inconnue': {
        status: 404,
        body: { status: 404, code: 'review.notion-not-found' },
      },
    })
    renderAt('/subjects/go/practice/inconnue', signedIn())

    expect(await screen.findByRole('heading', { name: 'Notion introuvable' })).toBeInTheDocument()
  })

  it('asks a visitor to sign in', async () => {
    stubApi({})
    renderAt('/subjects/go/practice/hello-world')

    expect(await screen.findByRole('heading', { name: 'Connexion requise' })).toBeInTheDocument()
  })
})
