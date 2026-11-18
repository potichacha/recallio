import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { QueuedCard, ReviewQueue } from '../api/review'
import type { CodeEditorProps } from '../components/CodeEditor'
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

// CodeMirror s'affiche sous jsdom (voir CodeEditor.test.tsx), mais on n'y tape pas de façon fiable : la saisie du
// parcours passe par une zone de texte de même nom. Playwright tape dans le vrai éditeur (e2e/tests/exercise.spec.ts).
vi.mock('../components/CodeEditor', () => ({
  default: ({ label, value, readOnly, onChange }: CodeEditorProps) => (
    <textarea
      aria-label={label}
      defaultValue={value}
      readOnly={readOnly}
      onChange={(event) => onChange?.(event.target.value)}
    />
  ),
}))

const QUEUE = '/api/v1/review-queue'
const REVIEWS = 'POST /api/v1/reviews'
const MINUTE = 60_000
/** Le nombre de tabulations au-delà duquel on renonce : la page a moins d'éléments que cela avant la carte. */
const MAX_TABS = 40

function counts(dueNow: number) {
  return { total: 40, seen: 10, mastered: 2, dueNow, dueToday: dueNow }
}

function subjectStats(slug: string, title: string, dueNow: number, newToday: number) {
  return {
    slug,
    title,
    counts: counts(dueNow),
    newToday,
    notions: { total: 0, discovered: 0, acquired: 0, mastered: 0, expert: 0 },
    readiness: { essentials: 0, acquired: 0, mastered: 0, percent: 0, ready: false },
    themes: [],
  }
}

/** Ce que charge l'écran de lancement : le profil, la progression, le catalogue et les cartes de chaque sujet suivi. */
const PROFILE = {
  ...SIGNED_IN_API,
  '/api/v1/subjects': { body: TEST_SUBJECTS },
  '/api/v1/me/review-stats': {
    body: {
      overall: counts(5),
      newToday: 30,
      subjects: [subjectStats('java', 'Java', 3, 20), subjectStats('go', 'Go', 2, 1)],
    },
  },
}
const FOLLOWING_JAVA_AND_GO = { body: { ...TEST_PROFILE, followedSubjects: ['java', 'go'] } }
const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

/** Une question de cours et ses choix (ADR 0051) : la bonne réponse, la machine virtuelle, est la première. */
function queuedCard(overrides: Partial<QueuedCard> = {}): QueuedCard {
  return {
    cardId: 'card-1',
    type: 'FLASHCARD',
    subjectSlug: 'java',
    subjectTitle: 'Java',
    themeSlug: 'java-fondamentaux',
    themeTitle: 'Java : fondamentaux',
    prompt: "Qu'est-ce que la JVM ?",
    choices: [
      'La machine virtuelle qui exécute le bytecode',
      'Le compilateur de Java',
      'Une bibliothèque de tests',
    ],
    multipleAnswers: false,
    blankCount: 0,
    exercise: null,
    notionSlug: null,
    notionTitle: null,
    lesson: null,
    state: 'NEW',
    ...overrides,
  }
}

function queue(cards: QueuedCard[], overrides: Partial<ReviewQueue> = {}): ReviewQueue {
  return { cards, dueCount: 0, newCount: cards.length, nextDueAt: null, ...overrides }
}

/** Une réponse juste, enregistrée : la note découle de la correction (ADR 0063). */
const recorded = {
  body: {
    cardId: 'card-1',
    rating: 'GOOD',
    state: 'LEARNING',
    dueAt: '2026-10-29T10:10:00Z',
    correction: {
      correctChoices: [0],
      blanks: [],
      explanation: 'La JVM exécute le bytecode.',
      exerciseRun: null,
      solution: [],
    },
  },
}

/** Lance une session depuis l'écran de lancement, avec les réglages déjà choisis. */
async function startSession() {
  await userEvent.click(await screen.findByRole('button', { name: 'Commencer' }))
}

/** Coche la machine virtuelle, la bonne réponse de la carte par défaut. */
async function chooseTheVirtualMachine() {
  await userEvent.click(await screen.findByRole('radio', { name: /machine virtuelle/ }))
}

/** Répond juste à la carte par défaut, puis passe à la suivante. */
async function answerCard() {
  await chooseTheVirtualMachine()
  await userEvent.click(screen.getByRole('button', { name: 'Valider' }))
  await userEvent.click(await screen.findByRole('button', { name: 'Carte suivante' }))
}

/** Appuie sur Tab jusqu'à ce que le focus soit sur l'élément attendu, comme au clavier seul. */
async function tabUntil(isTarget: (element: Element | null) => boolean) {
  for (let tabs = 0; tabs < MAX_TABS && !isTarget(document.activeElement); tabs++) {
    await userEvent.tab()
  }
  expect(isTarget(document.activeElement)).toBe(true)
}

function queueRequests(fetchMock: ReturnType<typeof stubApi>) {
  return sentRequests(fetchMock)
    .filter((request) => request.url.includes(QUEUE))
    .map((request) => new URL(request.url).searchParams)
}

beforeEach(() => localStorage.clear())
afterEach(() => vi.restoreAllMocks())

describe('ReviewPage', () => {
  it('asks anonymous visitors to sign in', () => {
    renderAt('/review')

    expect(screen.getByRole('heading', { name: 'Connexion requise' })).toBeInTheDocument()
  })

  it('opens on the subjects, by category, the ones in preparation greyed out', async () => {
    stubApi(PROFILE)
    renderAt('/review', signedIn())

    // Rust est en brouillon dans le catalogue des tests : il est annoncé, mais ne s'ouvre pas.
    const languages = await screen.findByRole('region', { name: 'Langages' })
    expect(within(languages).getByRole('link', { name: 'Java' })).toHaveAttribute(
      'href',
      '/review?subject=java',
    )
    const rust = within(languages).getByRole('button', { name: /Rust/ })
    expect(rust).toHaveTextContent('En préparation')
    await userEvent.click(rust)
    expect(rust).toHaveTextContent('Work in progress')
  })

  it('asks the length of a session on the chosen subject, and nothing else', async () => {
    stubApi({ ...PROFILE, '/api/v1/me': FOLLOWING_JAVA_AND_GO })
    renderAt('/review?subject=java', signedIn())

    expect(await screen.findByRole('heading', { name: 'Réviser Java' })).toBeInTheDocument()
    expect(await screen.findByText('3 à revoir · 20 nouvelles')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Changer de sujet/ })).toHaveAttribute(
      'href',
      '/review',
    )
    const durations = screen.getByRole('group', { name: 'Durée' })
    expect(
      within(durations)
        .getAllByRole('radio')
        .map((radio) => radio.closest('label')?.textContent),
    ).toEqual(['5 min', '10 min', '15 min', '20 min', 'Sans limite'])
    expect(within(durations).getByRole('radio', { name: '10 min' })).toBeChecked()
    // Le serveur corrige toutes les cartes (ADR 0063) : plus de questions à choisir.
    expect(screen.queryByRole('group', { name: 'Questions' })).not.toBeInTheDocument()
    expect(screen.queryByRole('radio', { name: /questions/i })).not.toBeInTheDocument()
  })

  it('starts a session on the subject chosen in the gallery, for the chosen length', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      '/api/v1/me': FOLLOWING_JAVA_AND_GO,
      [QUEUE]: { body: queue([queuedCard({ subjectSlug: 'go', subjectTitle: 'Go' })]) },
    })
    renderAt('/review', signedIn())

    await userEvent.click(await screen.findByRole('link', { name: 'Go' }))
    await userEvent.click(await screen.findByRole('radio', { name: '15 min' }))
    await startSession()

    expect(await screen.findByRole('heading', { name: 'Réviser : Go' })).toBeInTheDocument()
    expect(screen.getByRole('timer', { name: 'Temps restant' })).toHaveTextContent('Encore 15 min')
    const [query] = queueRequests(fetchMock)
    expect(query?.get('subject')).toBe('go')
    expect(query?.getAll('type')).toEqual([])
  })

  it('remembers the last length on this device, ignoring the questions an older version kept', async () => {
    localStorage.setItem(
      'recallio.review-session',
      JSON.stringify({ subject: 'go', minutes: null, mode: 'graded' }),
    )
    const fetchMock = stubApi({
      ...PROFILE,
      '/api/v1/me': FOLLOWING_JAVA_AND_GO,
      [QUEUE]: { body: queue([queuedCard()]) },
    })
    renderAt('/review?subject=go', signedIn())

    expect(await screen.findByRole('radio', { name: 'Sans limite' })).toBeChecked()
    await startSession()

    await screen.findByText("Qu'est-ce que la JVM ?")
    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
    expect(queueRequests(fetchMock)[0]?.getAll('type')).toEqual([])
  })

  it('follows a subject reviewed for the first time, so that it joins the dashboard', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      'PUT /api/v1/me/followed-subjects': {
        body: { ...TEST_PROFILE, followedSubjects: ['java', 'go'] },
      },
      [QUEUE]: { body: queue([queuedCard({ subjectSlug: 'go', subjectTitle: 'Go' })]) },
    })
    renderAt('/review?subject=go', signedIn())

    await startSession()

    expect(await screen.findByRole('heading', { name: 'Réviser : Go' })).toBeInTheDocument()
    const put = sentRequests(fetchMock).find((request) => request.method === 'PUT')
    expect(await put?.json()).toEqual({ subjects: ['java', 'go'] })
  })

  it('records the chosen answer, without any self-assessment, then shows the next card', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: {
        body: queue([queuedCard(), queuedCard({ cardId: 'card-2', prompt: 'Taille d’un int ?' })]),
      },
      [REVIEWS]: { status: 201, ...recorded },
    })
    renderAt('/review?subject=java', signedIn())
    await startSession()

    expect(await screen.findByText("Qu'est-ce que la JVM ?")).toBeInTheDocument()
    // Ni carte à retourner, ni zone de texte, ni « Je savais » : on choisit, le serveur corrige (ADR 0063).
    expect(screen.queryByRole('button', { name: 'Voir la réponse' })).not.toBeInTheDocument()
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Je savais' })).not.toBeInTheDocument()
    await answerCard()

    expect(await screen.findByText('Taille d’un int ?')).toBeInTheDocument()
    expect(screen.getByText('1 carte révisée')).toBeInTheDocument()
    const post = sentRequests(fetchMock).find((request) => request.method === 'POST')
    const body = await post?.json()
    expect(body).toMatchObject({ cardId: 'card-1', selectedChoices: [0] })
    expect(body.rating).toBeUndefined()
    expect(body.requestId).toMatch(/^[0-9a-f-]{36}$/)
    expect(body.elapsedMs).toBeGreaterThanOrEqual(0)
  })

  it('reports an error on the card being reviewed', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: { body: queue([queuedCard()]) },
      'POST /api/v1/content-reports': {
        status: 201,
        body: { id: 'r1', reason: 'TYPO', createdAt: '2026-11-05T08:00:00Z' },
      },
    })
    renderAt('/review?subject=java', signedIn())
    await startSession()

    await userEvent.click(await screen.findByRole('button', { name: 'Signaler une erreur' }))
    const form = screen.getByRole('form', { name: 'Signaler une erreur' })
    await userEvent.click(within(form).getByRole('radio', { name: 'Une faute de frappe' }))
    await userEvent.type(within(form).getByLabelText('Précisions (facultatif)'), '  « bytecode »  ')
    await userEvent.click(within(form).getByRole('button', { name: 'Envoyer' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Merci : le signalement est envoyé')
    const post = sentRequests(fetchMock).find((request) => request.url.endsWith('/content-reports'))
    expect(await post?.json()).toEqual({
      cardId: 'card-1',
      reason: 'TYPO',
      comment: '« bytecode »',
    })
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
    renderAt('/review?subject=java', signedIn())
    await startSession()

    await chooseTheVirtualMachine()
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Erreur temporaire')

    responses[REVIEWS] = {
      status: 201,
      ...recorded,
    } as unknown as (typeof responses)[typeof REVIEWS]
    await userEvent.click(screen.getByRole('button', { name: 'Réessayer' }))

    await waitFor(() =>
      expect(sentRequests(fetchMock).filter((request) => request.method === 'POST')).toHaveLength(
        2,
      ),
    )
    const posts = sentRequests(fetchMock).filter((request) => request.method === 'POST')
    const [first, second] = await Promise.all(posts.map((request) => request.json()))
    expect(second.requestId).toBe(first.requestId)
    expect(second.selectedChoices).toEqual([0])
  })

  it('fetches the next batch by itself once the batch is finished', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: { body: queue([queuedCard()]) },
      [REVIEWS]: { status: 201, ...recorded },
    })
    renderAt('/review?subject=java', signedIn())
    await startSession()

    await answerCard()

    await waitFor(() => expect(queueRequests(fetchMock)).toHaveLength(2))
    expect(await screen.findByText("Qu'est-ce que la JVM ?")).toBeInTheDocument()
  })

  it('tells when the subject is up to date, and when the next card is due', async () => {
    const nextDueAt = new Date(Date.now() + 10 * MINUTE).toISOString()
    stubApi({ ...PROFILE, [QUEUE]: { body: queue([], { nextDueAt }) } })
    renderAt('/review?subject=java', signedIn())
    await startSession()

    const summary = await screen.findByRole('region', { name: 'Bilan de la session' })
    expect(summary).toHaveTextContent('Tout est à jour en Java pour le moment.')
    expect(summary).toHaveTextContent('Prochaine carte à revoir dans 10 min.')
    // Qui prépare un entretien n'a pas à passer par le Tour.
    expect(
      within(summary).queryByRole('link', { name: 'Valide la notion suivante' }),
    ).not.toBeInTheDocument()
    expect(
      within(summary).queryByRole('button', { name: 'Relancer une session' }),
    ).not.toBeInTheDocument()
    await userEvent.click(within(summary).getByRole('button', { name: 'Choisir un autre sujet' }))

    expect(await screen.findByRole('button', { name: 'Commencer' })).toBeInTheDocument()
  })

  it('sends who learns to the Tour when nothing is left to review (ADR 0059)', async () => {
    stubApi({
      ...PROFILE,
      '/api/v1/me': { body: { ...TEST_PROFILE, tracks: { java: 'LEARN' } } },
      [QUEUE]: { body: queue([]) },
    })
    renderAt('/review?subject=java', signedIn())
    await startSession()

    const summary = await screen.findByRole('region', { name: 'Bilan de la session' })
    expect(summary).toHaveTextContent('Tu révises ce que tu as validé dans le Tour.')
    expect(
      await within(summary).findByRole('link', { name: 'Valide la notion suivante' }),
    ).toHaveAttribute('href', '/subjects/java/tour')
  })

  it('ends the session once its time is up, after the card being reviewed', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: { body: queue([queuedCard(), queuedCard({ cardId: 'card-2' })]) },
      [REVIEWS]: { status: 201, ...recorded },
    })
    renderAt('/review?subject=java', signedIn())
    await startSession()
    await chooseTheVirtualMachine()

    // Les dix minutes passent pendant qu'on répond : on finit la carte, puis vient le bilan.
    const later = Date.now() + 11 * MINUTE
    vi.spyOn(Date, 'now').mockReturnValue(later)
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Carte suivante' }))

    const summary = await screen.findByRole('region', { name: 'Bilan de la session' })
    expect(summary).toHaveTextContent('Temps écoulé : session terminée')
    expect(summary).toHaveTextContent('1 carte révisée, dont 1 réussie.')
    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
    await userEvent.click(within(summary).getByRole('button', { name: 'Relancer une session' }))

    expect(await screen.findByText("Qu'est-ce que la JVM ?")).toBeInTheDocument()
    expect(screen.getByText('0 carte révisée')).toBeInTheDocument()
    expect(queueRequests(fetchMock).length).toBeGreaterThanOrEqual(1)
  })

  it('lets the candidate end the session, then change its settings', async () => {
    stubApi({ ...PROFILE, [QUEUE]: { body: queue([queuedCard()]) } })
    renderAt('/review?subject=java', signedIn())
    await startSession()

    await userEvent.click(await screen.findByRole('button', { name: 'Terminer la session' }))

    const summary = screen.getByRole('region', { name: 'Bilan de la session' })
    expect(summary).toHaveTextContent('Session terminée')
    await userEvent.click(
      within(summary).getByRole('button', { name: 'Changer de sujet ou de durée' }),
    )
    expect(await screen.findByRole('group', { name: 'Durée' })).toBeInTheDocument()
  })

  it('reviews a single theme at once, without a length', async () => {
    const fetchMock = stubApi({ ...PROFILE, [QUEUE]: { body: queue([]) } })
    renderAt('/review?theme=java-fondamentaux', signedIn())

    const summary = await screen.findByRole('region', { name: 'Bilan de la session' })
    expect(summary).toHaveTextContent('Tout est à jour pour le moment.')
    expect(within(summary).getByRole('link', { name: 'Lancer une autre session' })).toHaveAttribute(
      'href',
      '/review',
    )
    expect(screen.queryByRole('button', { name: 'Commencer' })).not.toBeInTheDocument()
    expect(queueRequests(fetchMock)[0]?.get('theme')).toBe('java-fondamentaux')
  })

  it('reviews with a group at once, according to its settings', async () => {
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
    expect(screen.queryByRole('group', { name: 'Durée' })).not.toBeInTheDocument()
    expect(queueRequests(fetchMock)[0]?.get('group')).toBe('g1')
  })

  it('grades a multiple-choice card on the server, shows the correction, then moves on', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: {
        body: queue([
          queuedCard({
            type: 'MULTIPLE_CHOICE',
            prompt: 'Où vivent les objets créés avec `new` ?',
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
    renderAt('/review?subject=java', signedIn())
    await startSession()

    const validate = await screen.findByRole('button', { name: 'Valider' })
    expect(validate).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Voir la réponse' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('radio', { name: /Dans la pile/ }))
    await userEvent.click(validate)

    expect(await screen.findByRole('status')).toHaveTextContent('Pas tout à fait')
    expect(screen.getByText('tas')).toBeInTheDocument()
    // Le focus passe sur la suite : on enchaîne les cartes au clavier (ADR 0058).
    expect(screen.getByRole('button', { name: 'Carte suivante' })).toHaveFocus()
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

  it('offers to read the lesson again under a wrong answer, in another tab (audit, item 35)', async () => {
    stubApi({
      ...PROFILE,
      [QUEUE]: {
        body: queue([
          queuedCard({
            type: 'TRUE_FALSE',
            prompt: 'Le tas est partagé entre les threads.',
            choices: ['Vrai', 'Faux'],
            notionSlug: 'memoire-jvm',
            notionTitle: 'La mémoire de la JVM',
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
          correction: { correctChoices: [0], explanation: 'Oui.' },
        },
      },
    })
    renderAt('/review?subject=java', signedIn())
    await startSession()

    expect(screen.queryByRole('link', { name: /Revoir la leçon/ })).not.toBeInTheDocument()
    await userEvent.click(await screen.findByRole('radio', { name: /Faux/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))

    const lesson = await screen.findByRole('link', { name: /Revoir la leçon/ })
    expect(lesson).toHaveAttribute('href', '/subjects/java/course/memoire-jvm')
    expect(lesson).toHaveAttribute('target', '_blank')
  })

  it('leads to the mistakes of the week from the setup of a session, when there are some', async () => {
    stubApi({
      ...PROFILE,
      '/api/v1/me/mistakes/java': { body: { cards: [{ cardId: 'c1' }, { cardId: 'c2' }] } },
    })
    renderAt('/review?subject=java', signedIn())

    expect(
      await screen.findByRole('link', { name: 'Revoir mes erreurs de la semaine (2)' }),
    ).toHaveAttribute('href', '/subjects/java/mistakes')
  })

  it('grades a true-or-false card on the server like a two-choice question', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: {
        body: queue([
          queuedCard({
            type: 'TRUE_FALSE',
            prompt: '`new String("a") == "a"` vaut `true`.',
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
    renderAt('/review?subject=java', signedIn())
    await startSession()

    expect(await screen.findByText('Vrai ou faux')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('radio', { name: /Faux/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))

    expect(await screen.findByRole('status')).toBeInTheDocument()
    const post = sentRequests(fetchMock).find((request) => request.method === 'POST')
    const body = await post?.json()
    expect(body.selectedChoices).toEqual([1])
  })

  it('grades a course question on its choices, like a multiple-choice card', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: {
        body: queue([
          queuedCard({
            prompt: 'Java est-il compilé ou interprété ?',
            choices: ['Compilé', 'Interprété', 'Les deux'],
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
          correction: {
            correctChoices: [2],
            explanation: 'Compilé en bytecode, puis interprété et compilé à la volée par la JVM.',
          },
        },
      },
    })
    renderAt('/review?subject=java', signedIn())
    await startSession()

    await userEvent.click(await screen.findByRole('radio', { name: /Les deux/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Bonne réponse')
    expect(screen.getByText(/Compilé en bytecode/)).toBeInTheDocument()
    const post = sentRequests(fetchMock).find((request) => request.method === 'POST')
    const body = await post?.json()
    expect(body.selectedChoices).toEqual([2])
    expect(body.rating).toBeUndefined()
  })

  it('grades a bug to fix on its choices, its code shown in a block (ADR 0063)', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: {
        body: queue([
          queuedCard({
            type: 'BUG_FIX',
            prompt: 'Corrige ce code :\n\n```java\nif (s == "oui") { ok(); }\n```',
            choices: ['`if ("oui".equals(s))`', '`if (s === "oui")`', '`if (s.is("oui"))`'],
          }),
        ]),
      },
      [REVIEWS]: { status: 201, ...recorded },
    })
    renderAt('/review?subject=java', signedIn())
    await startSession()

    const card = await screen.findByRole('article')
    expect(within(card).getByText('Bug à corriger')).toBeInTheDocument()
    expect(card.querySelector('pre')).toHaveTextContent('if (s == "oui") { ok(); }')
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('radio', { name: /equals/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Bonne réponse')
    const post = sentRequests(fetchMock).find((request) => request.method === 'POST')
    const body = await post?.json()
    expect(body.selectedChoices).toEqual([0])
  })

  it('grades each blank of a fill-in card on the server and shows the expected answers', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: {
        body: queue([
          queuedCard({
            type: 'FILL_IN',
            prompt: '```java\npublic int ___() { return Objects.___(x, y); }\n```',
            choices: [],
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
    renderAt('/review?subject=java', signedIn())
    await startSession()

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

  it('says so, rather than breaking, when a card cannot be graded by the server', async () => {
    // Une carte sans choix, ni trous, ni tests : la file n'en sert plus (ADR 0063), mais l'écran ne doit pas casser.
    stubApi({ ...PROFILE, [QUEUE]: { body: queue([queuedCard({ choices: [] })]) } })
    renderAt('/review?subject=java', signedIn())
    await startSession()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Cette carte ne peut pas être corrigée',
    )
    expect(screen.getByRole('button', { name: 'Signaler une erreur' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Terminer la session' })).toBeInTheDocument()
  })

  it('shows the lesson of a notion before its first card, then the card', async () => {
    stubApi({
      ...PROFILE,
      [QUEUE]: {
        body: queue([
          queuedCard({
            notionTitle: 'La JVM',
            lesson: 'La **JVM** exécute le bytecode.',
          }),
        ]),
      },
    })
    renderAt('/review?subject=java', signedIn())
    await startSession()

    const lesson = await screen.findByRole('article', { name: 'Leçon : La JVM' })
    expect(within(lesson).getByText('JVM').tagName).toBe('STRONG')
    expect(screen.queryByText("Qu'est-ce que la JVM ?")).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Passer à la carte' }))

    expect(screen.getByText("Qu'est-ce que la JVM ?")).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Valider' })).toBeInTheDocument()
  })

  it('says which notion a card is about, or its theme when it has none', async () => {
    stubApi({
      ...PROFILE,
      [QUEUE]: { body: queue([queuedCard({ notionTitle: 'La JVM' }), queuedCard()]) },
    })
    renderAt('/review?subject=java', signedIn())
    await startSession()

    // Le sujet et le thème situent la carte ; sa notion, quand elle en a une, lui sert de titre.
    expect(await screen.findByText('Java · Java : fondamentaux')).toBeInTheDocument()
    expect(screen.getByText('La JVM')).toBeInTheDocument()
  })

  it('runs an exercise against its tests, then submits it and shows the solution', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: {
        body: queue([
          queuedCard({
            type: 'CODING_EXERCISE',
            prompt: 'Retirer les doublons.',
            choices: [],
            exercise: {
              starter: [{ path: 'Doublons.java', content: 'class Doublons {}' }],
              tests: [{ path: 'DoublonsTest.java', content: 'class DoublonsTest {}' }],
            },
          }),
        ]),
      },
      'POST /api/v1/exercise-runs': {
        body: {
          status: 'FAILED',
          tests: [
            {
              className: 'DoublonsTest',
              name: 'retireLesDoublons',
              passed: false,
              message: 'expected: <2> but was: <3>',
            },
          ],
          output: '',
        },
      },
      [REVIEWS]: {
        status: 201,
        body: {
          cardId: 'card-1',
          rating: 'GOOD',
          state: 'LEARNING',
          dueAt: '2026-10-29T10:10:00Z',
          correction: {
            correctChoices: [],
            blanks: [],
            explanation: 'Un LinkedHashSet garde l’ordre.',
            exerciseRun: {
              status: 'PASSED',
              tests: [
                { className: 'DoublonsTest', name: 'retireLesDoublons', passed: true, message: '' },
              ],
              output: '',
            },
            solution: [{ path: 'Doublons.java', content: 'class Doublons { /* solution */ }' }],
          },
        },
      },
    })
    renderAt('/review?subject=java', signedIn())
    await startSession()

    await userEvent.click(await screen.findByRole('tab', { name: /DoublonsTest\.java/ }))
    expect(await screen.findByRole('textbox', { name: 'DoublonsTest.java' })).toHaveAttribute(
      'readonly',
    )
    await userEvent.click(screen.getByRole('tab', { name: 'Doublons.java' }))
    const editor = await screen.findByRole('textbox', { name: 'Doublons.java' })
    await userEvent.clear(editor)
    await userEvent.type(editor, 'class Doublons {{}')
    await userEvent.click(screen.getByRole('button', { name: 'Lancer les tests' }))

    expect(await screen.findByText('expected: <2> but was: <3>')).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Solution' })).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Rendre' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Exercice réussi !')
    expect(screen.getByRole('region', { name: 'Solution' })).toHaveTextContent('solution')
    const posts = sentRequests(fetchMock).filter((request) => request.method === 'POST')
    const submitted = await posts[1]?.json()
    expect(submitted.submittedFiles).toEqual([
      { path: 'Doublons.java', content: 'class Doublons {}' },
    ])
    expect(submitted.rating).toBeUndefined()
  })

  it('shows the progress towards the daily goal, refreshed after each review', async () => {
    const responses = {
      ...PROFILE,
      [QUEUE]: { body: queue([queuedCard(), queuedCard({ cardId: 'card-2' })]) },
      [REVIEWS]: { status: 201, ...recorded },
    }
    const fetchMock = stubApi(responses)
    renderAt('/review?subject=java', signedIn())
    await startSession()

    const goal = await screen.findByRole('progressbar', { name: 'Objectif du jour' })
    expect(goal).toHaveAttribute('value', '4')
    expect(screen.getByText('4 / 10')).toBeInTheDocument()

    responses['/api/v1/me/progress'] = {
      body: { ...TEST_PROGRESS, cardsToday: 10, goalReachedToday: true, streak: 4 },
    }
    await answerCard()

    expect(
      await screen.findByText('Objectif atteint ! Ta série passe à 4 jours.'),
    ).toBeInTheDocument()
    expect(
      sentRequests(fetchMock).filter((request) => request.url.endsWith('/api/v1/me/progress'))
        .length,
    ).toBeGreaterThanOrEqual(2)
  })

  it('starts at once from a link that gives the subject and the length', async () => {
    const fetchMock = stubApi({ ...PROFILE, [QUEUE]: { body: queue([queuedCard()]) } })
    renderAt('/review?subject=java&minutes=5', signedIn())

    expect(await screen.findByText("Qu'est-ce que la JVM ?")).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Commencer' })).not.toBeInTheDocument()
    expect(screen.getByRole('timer', { name: 'Temps restant' })).toHaveTextContent('Encore 5 min')
    const [query] = queueRequests(fetchMock)
    expect(query?.get('subject')).toBe('java')
    expect(query?.getAll('type')).toEqual([])
  })

  it('still starts at once from an older link that chose the questions, now ignored', async () => {
    const fetchMock = stubApi({ ...PROFILE, [QUEUE]: { body: queue([queuedCard()]) } })
    renderAt('/review?subject=java&minutes=5&mode=graded', signedIn())

    expect(await screen.findByText("Qu'est-ce que la JVM ?")).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Commencer' })).not.toBeInTheDocument()
    expect(queueRequests(fetchMock)[0]?.getAll('type')).toEqual([])
  })

  it('answers a card with the keyboard alone, then moves on with Enter (ADR 0058)', async () => {
    const fetchMock = stubApi({
      ...PROFILE,
      [QUEUE]: {
        body: queue([queuedCard(), queuedCard({ cardId: 'card-2', prompt: 'Taille d’un int ?' })]),
      },
      [REVIEWS]: { status: 201, ...recorded },
    })
    renderAt('/review?subject=java', signedIn())
    await startSession()
    expect(await screen.findByText("Qu'est-ce que la JVM ?")).toBeInTheDocument()

    // Tab jusqu'à un choix, Espace pour le cocher, Tab jusqu'à « Valider », Entrée.
    await tabUntil((element) => element instanceof HTMLInputElement && element.type === 'radio')
    await userEvent.keyboard(' ')
    const validate = screen.getByRole('button', { name: 'Valider' })
    await tabUntil((element) => element === validate)
    await userEvent.keyboard('{Enter}')

    // Le focus est passé sur « Carte suivante » : Entrée ouvre la carte d'après.
    expect(await screen.findByRole('button', { name: 'Carte suivante' })).toHaveFocus()
    await userEvent.keyboard('{Enter}')

    expect(await screen.findByText('Taille d’un int ?')).toBeInTheDocument()
    const post = sentRequests(fetchMock).find((request) => request.method === 'POST')
    const body = await post?.json()
    expect(body.selectedChoices).toHaveLength(1)
  })

  it('offers five more minutes once the time is up', async () => {
    stubApi({
      ...PROFILE,
      [QUEUE]: { body: queue([queuedCard(), queuedCard({ cardId: 'card-2' })]) },
      [REVIEWS]: { status: 201, ...recorded },
    })
    renderAt('/review?subject=java', signedIn())
    await startSession()
    await chooseTheVirtualMachine()
    vi.spyOn(Date, 'now').mockReturnValue(Date.now() + 11 * MINUTE)
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Carte suivante' }))

    const summary = await screen.findByRole('region', { name: 'Bilan de la session' })
    await userEvent.click(within(summary).getByRole('button', { name: 'Encore 5 minutes' }))

    expect(await screen.findByText("Qu'est-ce que la JVM ?")).toBeInTheDocument()
    expect(screen.getByRole('timer', { name: 'Temps restant' })).toHaveTextContent('Encore 5 min')
  })
})
