import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { QueuedCard, ReviewQueue } from '../api/review'
import {
  fakeSession,
  renderAt,
  sentRequests,
  SIGNED_IN_API,
  stubApi,
  TEST_GROUP_SETTINGS,
  TEST_PROFILE,
  TEST_PROGRESS,
  TEST_SUBJECTS,
} from '../test/render'

const QUEUE = '/api/v1/review-queue'
const REVIEWS = 'POST /api/v1/reviews'
const PROFILE = SIGNED_IN_API
const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const MINUTE = 60_000
const DAY = 24 * 60 * MINUTE

function queuedCard(overrides: Partial<QueuedCard> = {}): QueuedCard {
  const inMs = (delay: number) => new Date(Date.now() + delay).toISOString()
  return {
    cardId: 'card-1',
    type: 'FLASHCARD',
    subjectSlug: 'java',
    subjectTitle: 'Java',
    themeSlug: 'java-fondamentaux',
    themeTitle: 'Java : fondamentaux',
    prompt: "Qu'est-ce que la JVM ?",
    answer: 'La machine virtuelle qui exécute le bytecode.',
    choices: [],
    multipleAnswers: false,
    blankCount: 0,
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

  it('reviews with a group, without the tabs of the followed subjects', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: { body: queue([queuedCard()]) },
      '/api/v1/groups/g1': {
        body: {
          id: 'g1',
          name: 'Équipe backend',
          createdAt: '2026-11-01T10:00:00Z',
          myRole: 'MEMBER',
          settings: TEST_GROUP_SETTINGS,
          members: [],
        },
      },
    })
    renderAt('/review?group=g1', signedIn())

    expect(
      await screen.findByRole('heading', { name: 'Réviser avec « Équipe backend »' }),
    ).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Sujet révisé' })).not.toBeInTheDocument()
    const request = sentRequests(fetchMock).find((sent) => sent.url.includes(QUEUE))
    expect(new URL(request?.url ?? '').searchParams.get('group')).toBe('g1')
  })

  it('grades a multiple-choice card on the server, shows the correction, then moves on', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: {
        body: queue([
          queuedCard({
            type: 'MULTIPLE_CHOICE',
            prompt: 'Où vivent les objets créés avec `new` ?',
            answer: null,
            choices: ['Dans la pile', 'Dans le tas', 'Dans le Metaspace'],
          }),
          queuedCard({ cardId: 'card-2', prompt: 'Taille d’un int ?' }),
        ]),
      },
      [REVIEWS]: {
        status: 201,
        body: {
          cardId: 'card-1',
          rating: 'AGAIN',
          state: 'LEARNING',
          dueAt: '2026-10-29T10:01:00Z',
          correction: {
            correctChoices: [1],
            explanation: 'Les objets sont alloués dans le **tas**.',
          },
        },
      },
    })
    renderAt('/review', signedIn())

    const validate = await screen.findByRole('button', { name: 'Valider' })
    expect(validate).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Voir la réponse' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('radio', { name: /Dans la pile/ }))
    await userEvent.click(validate)

    expect(await screen.findByRole('status')).toHaveTextContent('Pas tout à fait')
    expect(screen.getByText('tas')).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: /Dans le tas/ }).closest('label')).toHaveTextContent(
      'Bonne réponse, non cochée',
    )
    const post = sentRequests(fetchMock).find((request) => request.method === 'POST')
    const body = await post?.json()
    expect(body.selectedChoices).toEqual([0])
    expect(body.rating).toBeUndefined()

    await userEvent.click(screen.getByRole('button', { name: 'Carte suivante' }))
    expect(await screen.findByText('Taille d’un int ?')).toBeInTheDocument()
  })

  it('grades a true-or-false card on the server like a two-choice question', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: {
        body: queue([
          queuedCard({
            type: 'TRUE_FALSE',
            prompt: '`new String("a") == "a"` vaut `true`.',
            answer: null,
            choices: ['Vrai', 'Faux'],
          }),
        ]),
      },
      [REVIEWS]: {
        status: 201,
        body: {
          cardId: 'card-1',
          rating: 'GOOD',
          state: 'LEARNING',
          dueAt: '2026-10-29T10:10:00Z',
          correction: { correctChoices: [1], explanation: '`new` crée toujours un nouvel objet.' },
        },
      },
    })
    renderAt('/review', signedIn())

    expect(await screen.findByText('Vrai ou faux')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('radio', { name: /Faux/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))

    expect(await screen.findByRole('status')).toBeInTheDocument()
    const post = sentRequests(fetchMock).find((request) => request.method === 'POST')
    const body = await post?.json()
    expect(body.selectedChoices).toEqual([1])
  })

  it('grades each blank of a fill-in card on the server and shows the expected answers', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: {
        body: queue([
          queuedCard({
            type: 'FILL_IN',
            prompt: '```java\npublic int ___() { return Objects.___(x, y); }\n```',
            answer: null,
            blankCount: 2,
          }),
        ]),
      },
      [REVIEWS]: {
        status: 201,
        body: {
          cardId: 'card-1',
          rating: 'AGAIN',
          state: 'LEARNING',
          dueAt: '2026-10-29T10:01:00Z',
          correction: {
            correctChoices: [],
            blanks: [
              { expected: 'hashCode', correct: true },
              { expected: 'hash', correct: false },
            ],
            explanation: 'Les mêmes champs qu’equals.',
          },
        },
      },
    })
    renderAt('/review', signedIn())

    // Le code est découpé en éléments par la coloration syntaxique : on lit le texte de toute la carte.
    expect(await screen.findByRole('article')).toHaveTextContent(
      'public int [1]() { return Objects.[2](x, y); }',
    )
    const validate = screen.getByRole('button', { name: 'Valider' })
    await userEvent.type(screen.getByRole('textbox', { name: /Trou 1/ }), 'hashCode')
    expect(validate).toBeDisabled()
    await userEvent.type(screen.getByRole('textbox', { name: /Trou 2/ }), 'combine')
    await userEvent.click(validate)

    expect(await screen.findByRole('status')).toHaveTextContent('Pas tout à fait')
    expect(screen.getByText('Attendu : hash')).toBeInTheDocument()
    const post = sentRequests(fetchMock).find((request) => request.method === 'POST')
    const body = await post?.json()
    expect(body.filledBlanks).toEqual(['hashCode', 'combine'])
    expect(body.rating).toBeUndefined()
  })

  it('lets the candidate write a fix to a bug before revealing the corrected code', async () => {
    stubApi({
      ...PROFILE,
      [QUEUE]: { body: queue([queuedCard({ type: 'BUG_FIX', answer: 'Utiliser `equals`.' })]) },
    })
    renderAt('/review', signedIn())

    const draft = await screen.findByRole('textbox', { name: 'Votre correction (facultatif)' })
    await userEvent.type(draft, 'if ("oui".equals(s))')
    await userEvent.click(screen.getByRole('button', { name: 'Voir la réponse' }))

    expect(draft).toHaveValue('if ("oui".equals(s))')
    expect(screen.getByRole('group', { name: 'Noter la carte' })).toBeInTheDocument()
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

  it('shows the progress towards the daily goal, refreshed after each review', async () => {
    const responses = {
      ...PROFILE,
      [QUEUE]: { body: queue([queuedCard(), queuedCard({ cardId: 'card-2' })]) },
      [REVIEWS]: { status: 201, ...recorded },
    }
    const fetchMock = stubApi(responses)
    renderAt('/review', signedIn())

    const goal = await screen.findByRole('progressbar', { name: 'Objectif du jour' })
    expect(goal).toHaveAttribute('value', '4')
    expect(screen.getByText('4 / 10')).toBeInTheDocument()

    responses['/api/v1/me/progress'] = {
      body: { ...TEST_PROGRESS, cardsToday: 10, goalReachedToday: true, streak: 4 },
    }
    await userEvent.click(screen.getByRole('button', { name: 'Voir la réponse' }))
    await userEvent.click(screen.getByRole('button', { name: 'Bien' }))

    expect(
      await screen.findByText('Objectif atteint ! Votre série passe à 4 jours.'),
    ).toBeInTheDocument()
    expect(
      sentRequests(fetchMock).filter((request) => request.url.endsWith('/api/v1/me/progress'))
        .length,
    ).toBeGreaterThanOrEqual(2)
  })

  it('reviews all followed subjects, or one of them chosen in the tabs', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      '/api/v1/me': { body: { ...TEST_PROFILE, followedSubjects: ['java', 'go'] } },
      '/api/v1/subjects': { body: TEST_SUBJECTS },
      [QUEUE]: { body: queue([queuedCard()]) },
    })
    renderAt('/review', signedIn())

    const tabs = await screen.findByRole('navigation', { name: 'Sujet révisé' })
    expect(within(tabs).getByRole('link', { name: 'Tous mes sujets' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    await userEvent.click(await within(tabs).findByRole('link', { name: 'Go' }))

    await waitFor(() =>
      expect(
        sentRequests(fetchMock).some(
          (request) => new URL(request.url).searchParams.get('subject') === 'go',
        ),
      ).toBe(true),
    )
    expect(within(tabs).getByRole('link', { name: 'Go' })).toHaveAttribute('aria-current', 'page')
  })

  it('does not show the tabs to a user who follows a single subject', async () => {
    stubApi({ ...PROFILE, [QUEUE]: { body: queue([queuedCard()]) } })
    renderAt('/review', signedIn())

    expect(await screen.findByText("Qu'est-ce que la JVM ?")).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Sujet révisé' })).not.toBeInTheDocument()
  })
})
