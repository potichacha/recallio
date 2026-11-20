import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { DailyChallenge, DailyChallengeAnswerResult } from '../api/dailyChallenges'
import {
  fakeSession,
  renderAt,
  sentRequests,
  SIGNED_IN_API,
  stubApi,
  TEST_SUBJECTS,
} from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const API = { ...SIGNED_IN_API, '/api/v1/subjects': { body: TEST_SUBJECTS } }
const CHALLENGE = '/api/v1/me/daily-challenges/go'
const ANSWERS = 'POST /api/v1/me/daily-challenges/d1/answers'

/** Un défi court, de deux questions : un vrai ou faux, puis un code à compléter. */
function challenge(overrides: Partial<DailyChallenge> = {}): DailyChallenge {
  return {
    id: 'd1',
    subjectSlug: 'go',
    subjectTitle: 'Go',
    day: '2026-11-20',
    state: 'NOT_STARTED',
    questionCount: 2,
    questions: [
      {
        position: 0,
        cardId: 'c1',
        type: 'TRUE_FALSE',
        prompt: '`main` prend des paramètres.',
        choices: ['Vrai', 'Faux'],
        multipleAnswers: false,
        blankCount: 0,
        notionSlug: 'hello-world',
        notionTitle: 'Hello, World',
      },
      {
        position: 1,
        cardId: 'c2',
        type: 'FILL_IN',
        prompt: '`var n int` vaut `___`',
        choices: [],
        multipleAnswers: false,
        blankCount: 1,
        notionSlug: 'valeur-zero',
        notionTitle: 'La valeur zéro',
      },
    ],
    answers: [],
    ...overrides,
  }
}

const STATS = {
  players: 3,
  averageScore: 1.3,
  distribution: [
    { score: 0, players: 1 },
    { score: 1, players: 1 },
    { score: 2, players: 1 },
  ],
}

const FIRST: DailyChallengeAnswerResult = {
  correction: {
    position: 0,
    correct: false,
    correctChoices: [1],
    blanks: [],
    explanation: 'Non : `main` ne prend rien.',
  },
  state: 'IN_PROGRESS',
  questionCount: 2,
  stats: null,
}

const LAST: DailyChallengeAnswerResult = {
  correction: {
    position: 1,
    correct: true,
    correctChoices: [],
    blanks: [{ expected: '0', correct: true }],
    explanation: 'La valeur zéro d’un int.',
  },
  state: 'DONE',
  score: 1,
  questionCount: 2,
  stats: STATS,
}

/** Le même défi, fini : une erreur, puis une réussite. */
const DONE = challenge({
  state: 'DONE',
  score: 1,
  answers: [
    {
      position: 0,
      correct: false,
      correctChoices: [1],
      expectedBlanks: [],
      explanation: 'Non : `main` ne prend rien.',
    },
    {
      position: 1,
      correct: true,
      correctChoices: [],
      expectedBlanks: ['0'],
      explanation: 'La valeur zéro d’un int.',
    },
  ],
  stats: STATS,
})

describe('DailyChallengePage (ADR 0067)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
    Reflect.deleteProperty(navigator, 'share')
  })

  it('tells a visitor what the challenge is, and invites to sign in or try without an account', async () => {
    stubApi({ '/api/v1/subjects': { body: TEST_SUBJECTS } })
    const session = fakeSession()
    renderAt('/daily/go', session)

    expect(await screen.findByRole('heading', { name: 'Défi du jour : Go' })).toBeInTheDocument()
    expect(screen.getByText('Une seule tentative par jour : chaque réponse compte.')).toBeVisible()
    expect(screen.getByRole('link', { name: 'Essayer sans compte' })).toHaveAttribute(
      'href',
      '/try/go',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Connecte-toi pour jouer' }))
    expect(session.login).toHaveBeenCalledWith('/daily/go')
  })

  it('asks the questions one by one, corrects each at once, then shows the result', async () => {
    const fetchMock = stubApi({
      ...API,
      [CHALLENGE]: { body: challenge() },
      '/api/v1/me/daily-challenges': { body: [] },
      [ANSWERS]: { body: FIRST },
    })
    renderAt('/daily/go', signedIn())

    expect(await screen.findByText('Question 1 sur 2')).toBeInTheDocument()
    expect(screen.getByText('Défi du 20 novembre : une seule réponse par question.')).toBeVisible()
    const validate = screen.getByRole('button', { name: 'Valider' })
    expect(validate).toBeDisabled()
    await userEvent.click(screen.getByRole('radio', { name: 'Vrai' }))
    await userEvent.click(validate)

    expect(await screen.findByText('Pas tout à fait.')).toBeInTheDocument()
    expect(screen.getByText(/ne prend rien/)).toBeVisible()
    expect(screen.getByRole('link', { name: /Revoir la leçon/ })).toHaveAttribute(
      'href',
      '/subjects/go/course/hello-world',
    )
    // Au clavier : le focus passe sur la question suivante, Entrée y mène.
    expect(screen.getByRole('button', { name: 'Question suivante' })).toHaveFocus()
    stubApi({
      ...API,
      '/api/v1/me/daily-challenges': { body: [] },
      [ANSWERS]: { body: LAST },
    })
    await userEvent.keyboard('{Enter}')

    expect(await screen.findByText('Question 2 sur 2')).toBeInTheDocument()
    const marks = screen.getByRole('list', { name: 'Tes réponses' })
    expect(
      within(marks)
        .getAllByRole('listitem')
        .map((mark) => mark.textContent),
    ).toEqual(['Question 1 : fausse', 'Question 2 : à venir'])
    await userEvent.type(screen.getByRole('textbox', { name: 'Trou 1' }), '0')
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))
    expect(await screen.findByText('Juste !')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Voir mon résultat' }))

    const result = await screen.findByRole('region', { name: 'Défi terminé' })
    expect(result).toHaveTextContent('1/2')
    expect(within(result).getByText('Reviens demain pour le prochain défi.')).toBeVisible()

    const [first] = sentRequests(fetchMock).filter((request) => request.method === 'POST')
    const body = await first?.json()
    expect(body).toEqual({
      requestId: expect.stringMatching(/^[0-9a-f-]{36}$/),
      position: 0,
      answer: { selectedChoices: [0] },
    })
  })

  it('shows the score, the marks, the figures of the day, and shares without the answers', async () => {
    stubApi({ ...API, [CHALLENGE]: { body: DONE } })
    // jsdom ne dessine pas : sans image, le partage se rabat sur le texte, celui qu'on vérifie ici.
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null)
    const share = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'share', { value: share, configurable: true })
    renderAt('/daily/go', signedIn())

    const result = await screen.findByRole('region', { name: 'Défi terminé' })
    expect(result).toHaveTextContent('1/2')
    const marks = within(result).getByRole('list', { name: 'Tes réponses' })
    expect(
      within(marks)
        .getAllByRole('listitem')
        .map((mark) => mark.textContent),
    ).toEqual(['Question 1 : fausse', 'Question 2 : juste'])

    const figures = within(result).getByRole('region', { name: 'Les chiffres du jour' })
    expect(figures).toHaveTextContent('3 joueurs ont fini ce défi · score moyen : 1,3 sur 2')
    const bars = within(figures).getByRole('list', { name: 'Répartition des scores' })
    expect(
      within(bars)
        .getAllByRole('listitem')
        .map((bar) => bar.textContent),
    ).toEqual(['2/21 joueur', '1/21 joueur', '0/21 joueur'])

    await userEvent.click(within(result).getByRole('button', { name: 'Partager' }))
    expect(share).toHaveBeenCalledWith({
      title: '1/2',
      text: 'J’ai fait 1/2 au défi Go du jour sur Recallio. À ton tour :',
      url: expect.stringMatching(/\/daily\/go$/),
    })
    const shared = JSON.stringify(share.mock.calls[0])
    expect(shared).not.toContain('main')
    expect(shared).not.toContain('valeur')
  })

  it('shows the result again on a return after the end, never a second attempt', async () => {
    stubApi({ ...API, [CHALLENGE]: { body: DONE } })
    renderAt('/daily/go', signedIn())

    expect(await screen.findByRole('region', { name: 'Défi terminé' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Valider' })).not.toBeInTheDocument()
    expect(screen.queryByText(/^Question \d sur/)).not.toBeInTheDocument()
  })

  it('celebrates a flawless challenge', async () => {
    const flawless = challenge({
      state: 'DONE',
      score: 2,
      answers: DONE.answers.map((answer) => ({ ...answer, correct: true })),
      stats: STATS,
    })
    stubApi({ ...API, [CHALLENGE]: { body: flawless } })
    renderAt('/daily/go', signedIn())

    expect(await screen.findByRole('region', { name: 'Sans faute !' })).toHaveTextContent('2/2')
  })

  it('says when the subject has no daily challenge', async () => {
    stubApi({
      ...API,
      '/api/v1/me/daily-challenges/kafka': {
        status: 422,
        body: {
          status: 422,
          title: 'Requête refusée',
          code: 'daily-challenge.unavailable',
          detail: 'Le sujet « kafka » n’a pas encore assez de questions pour un défi du jour.',
        },
      },
    })
    renderAt('/daily/kafka', signedIn())

    expect(await screen.findByText('Pas de défi du jour pour ce sujet')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Voir les autres défis' })).toHaveAttribute(
      'href',
      '/play',
    )
  })
})
