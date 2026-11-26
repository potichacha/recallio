import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { QuestBoard } from '../api/quests'
import type { ReviewStats } from '../api/stats'
import type { TourOutline } from '../api/tour'
import {
  fakeSession,
  renderAt,
  SIGNED_IN_API,
  stubApi,
  TEST_PROFILE,
  TEST_SUBJECTS,
} from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const counts = (overrides: Partial<ReviewStats['overall']> = {}) => ({
  total: 0,
  seen: 0,
  mastered: 0,
  dueNow: 0,
  dueToday: 0,
  ...overrides,
})

const NO_NOTIONS = { total: 0, discovered: 0, acquired: 0, mastered: 0, expert: 0 }
const NO_READINESS = { essentials: 0, acquired: 0, mastered: 0, percent: 0, ready: false }

function subjectStats(
  slug: string,
  title: string,
  overrides: Partial<ReviewStats['subjects'][number]> = {},
): ReviewStats['subjects'][number] {
  return {
    slug,
    title,
    counts: counts({ total: 30, seen: 12, mastered: 4, dueNow: 5 }),
    newToday: 8,
    notions: NO_NOTIONS,
    readiness: NO_READINESS,
    themes: [
      { slug: `${slug}-bases`, title: 'Les bases', counts: counts({ total: 10, seen: 10 }) },
      { slug: `${slug}-objet`, title: 'L’objet', counts: counts({ total: 12, seen: 2 }) },
      { slug: `${slug}-flux`, title: 'Les flux', counts: counts({ total: 8, seen: 0 }) },
    ],
    ...overrides,
  }
}

function stats(overrides: Partial<ReviewStats> = {}): ReviewStats {
  return {
    overall: counts({ total: 30, seen: 12, mastered: 4, dueNow: 5, dueToday: 7 }),
    newToday: 8,
    subjects: [subjectStats('java', 'Java')],
    ...overrides,
  }
}

const QUESTS: QuestBoard = {
  daily: [
    { quest: 'DAILY_REVIEWS', target: 15, progress: 15, xp: 20, completed: true },
    { quest: 'DAILY_CODE', target: 3, progress: 1, xp: 25, completed: false },
    { quest: 'DAILY_TOUR_NOTION', target: 1, progress: 0, xp: 20, completed: false },
  ],
  dailyEndsOn: '2026-10-30',
  weekly: [{ quest: 'WEEKLY_DAYS', target: 5, progress: 4, xp: 60, completed: false }],
  weeklyEndsOn: '2026-11-02',
}

const entry = (slug: string, title: string, pageCount = 1) => ({ slug, title, pageCount })

/** Le Tour de Java des tests : un premier chapitre d'une notion, puis « Les collections » et ses cinq notions. */
const TOUR: TourOutline = {
  subjectSlug: 'java',
  subjectTitle: 'Java',
  domains: [
    {
      id: 'bases',
      title: 'Les bases',
      chapters: [
        {
          id: 'premiers-pas',
          title: 'Premiers pas',
          notions: [entry('bienvenue', 'Bienvenue')],
          exercises: [],
        },
        {
          id: 'collections',
          title: 'Les collections',
          notions: [
            entry('list', 'List et ArrayList'),
            entry('parcourir', 'Parcourir une liste'),
            entry('set', 'Set et HashSet', 2),
            entry('map', 'Map et HashMap'),
            entry('deque', 'Deque'),
          ],
          exercises: [],
        },
      ],
    },
  ],
}

const TOUR_OUTLINE = '/api/v1/subjects/java/tour'
const TOUR_PROGRESS = '/api/v1/me/tour/java'
/** Trois notions validées, et la deuxième page de « Set et HashSet » ouverte en dernier. */
const IN_THE_COLLECTIONS = {
  validatedNotions: ['bienvenue', 'list', 'parcourir'],
  knownNotions: [],
  lastPage: { notion: 'set', page: 2 },
}
const NEVER_OPENED = { validatedNotions: [], knownNotions: [], lastPage: null }

function stubHome(
  reviewStats: ReviewStats = stats(),
  extra: Record<string, { status?: number; body: unknown }> = {},
) {
  return stubApi({
    ...SIGNED_IN_API,
    '/api/v1/subjects': { body: TEST_SUBJECTS },
    '/api/v1/me/review-stats': { body: reviewStats },
    '/api/v1/me/quests': { body: QUESTS },
    [TOUR_OUTLINE]: { body: TOUR },
    [TOUR_PROGRESS]: { body: IN_THE_COLLECTIONS },
    ...extra,
  })
}

describe('the home of a signed-in user (ADR 0077)', () => {
  it('replaces the landing page once signed in', async () => {
    stubHome()
    renderAt('/', signedIn())

    expect(await screen.findByRole('heading', { level: 1, name: 'Salut Alice' })).toBeVisible()
    expect(screen.queryByRole('region', { name: 'Essaie tout de suite' })).not.toBeInTheDocument()
  })

  it('says a word about the day: what is left to keep the streak', async () => {
    stubHome()
    renderAt('/', signedIn())

    expect(
      await screen.findByText('Encore 6 cartes et ta série passe à 4 jours.'),
    ).toBeInTheDocument()
  })

  it('leads to the reviews of the day, with the cards due in every followed subject', async () => {
    stubHome()
    renderAt('/', signedIn())

    const reviews = await screen.findByRole('link', { name: /5 cartes à revoir/ })
    expect(reviews).toHaveAttribute('href', '/review')
    expect(reviews).toHaveTextContent('Révisions du jour')
  })

  it('writes a single due card in the singular', async () => {
    stubHome(stats({ overall: counts({ total: 30, seen: 12, dueNow: 1 }) }))
    renderAt('/', signedIn())

    expect(await screen.findByRole('link', { name: /1 carte à revoir/ })).toBeInTheDocument()
  })

  it('shows no card for the reviews of the day when nothing is due', async () => {
    stubHome(stats({ overall: counts({ total: 30, seen: 12, dueNow: 0 }) }))
    renderAt('/', signedIn())

    await screen.findByRole('region', { name: 'Les collections' })
    expect(screen.queryByRole('link', { name: /à revoir/ })).not.toBeInTheDocument()
    expect(screen.queryByText('Révisions du jour')).not.toBeInTheDocument()
  })

  it('continues the lesson of the current subject where « Reprendre le Tour » leads', async () => {
    stubHome()
    renderAt('/', signedIn())

    // Le chapitre de la dernière page ouverte, et ses notions validées dans le Tour.
    const learn = await screen.findByRole('region', { name: 'Les collections' })
    expect(learn).toHaveTextContent('Apprendre · Java')
    expect(within(learn).getByRole('img', { name: 'Leçons faites : 2 sur 5' })).toBeInTheDocument()
    expect(learn).toHaveTextContent('2 sur 5')
    expect(learn).toHaveTextContent('Prochaine leçon : Set et HashSet.')
    expect(within(learn).getByRole('link', { name: 'Continuer' })).toHaveAttribute(
      'href',
      '/subjects/java/tour/set?page=2',
    )
  })

  it('counts a notion known at the level check as a lesson done (ADR 0068)', async () => {
    stubHome(stats(), {
      [TOUR_PROGRESS]: {
        body: {
          validatedNotions: ['list'],
          knownNotions: ['bienvenue', 'parcourir'],
          lastPage: null,
        },
      },
    })
    renderAt('/', signedIn())

    // « Commencer le Tour » saute les bases connues : la première notion à découvrir est « Set et HashSet ».
    const learn = await screen.findByRole('region', { name: 'Les collections' })
    expect(within(learn).getByRole('img', { name: 'Leçons faites : 2 sur 5' })).toBeInTheDocument()
    expect(within(learn).getByRole('link', { name: 'Continuer' })).toHaveAttribute(
      'href',
      '/subjects/java/tour/set',
    )
  })

  it('starts the Tour of a subject never opened, from its first lesson', async () => {
    stubHome(stats(), { [TOUR_PROGRESS]: { body: NEVER_OPENED } })
    renderAt('/', signedIn())

    const learn = await screen.findByRole('region', { name: 'Premiers pas' })
    expect(within(learn).getByRole('img', { name: 'Leçons faites : 0 sur 1' })).toBeInTheDocument()
    expect(within(learn).getByRole('link', { name: 'Commencer' })).toHaveAttribute(
      'href',
      '/subjects/java/tour/bienvenue',
    )
  })

  it('leads to the course of a subject without a Tour, counting its chapters', async () => {
    stubHome(stats(), {
      '/api/v1/subjects': { body: [{ ...TEST_SUBJECTS[0], hasTour: false }] },
    })
    renderAt('/', signedIn())

    // Le premier chapitre dont toutes les cartes n'ont pas été vues ; un chapitre est fait quand elles l'ont toutes été.
    const learn = await screen.findByRole('region', { name: 'L’objet' })
    expect(
      within(learn).getByRole('img', { name: 'Chapitres terminés : 1 sur 3' }),
    ).toBeInTheDocument()
    expect(within(learn).getByRole('link', { name: 'Continuer' })).toHaveAttribute(
      'href',
      '/subjects/java/course',
    )
  })

  it('falls back on the course when the Tour does not load', async () => {
    stubHome(stats(), { [TOUR_OUTLINE]: { status: 500, body: { status: 500 } } })
    renderAt('/', signedIn())

    const learn = await screen.findByRole('region', { name: 'L’objet' })
    expect(within(learn).getByRole('link', { name: 'Continuer' })).toHaveAttribute(
      'href',
      '/subjects/java/course',
    )
  })

  it('puts forward the subject of the last session on this device', async () => {
    localStorage.setItem('recallio.review-session', JSON.stringify({ subject: 'go', minutes: 5 }))
    stubHome(stats({ subjects: [subjectStats('java', 'Java'), subjectStats('go', 'Go')] }), {
      '/api/v1/me': { body: { ...TEST_PROFILE, followedSubjects: ['java', 'go'] } },
      '/api/v1/subjects/go/tour': { body: { ...TOUR, subjectSlug: 'go', subjectTitle: 'Go' } },
      '/api/v1/me/tour/go': { body: NEVER_OPENED },
    })
    renderAt('/', signedIn())

    const learn = await screen.findByRole('region', { name: 'Premiers pas' })
    expect(learn).toHaveTextContent('Apprendre · Go')
    expect(within(learn).getByRole('link', { name: 'Commencer' })).toHaveAttribute(
      'href',
      '/subjects/go/tour/bienvenue',
    )
  })

  it('opens the two other doors from their tile', async () => {
    stubHome()
    renderAt('/', signedIn())

    // Dans la page : la navigation porte aussi ces deux noms.
    const main = within(screen.getByRole('main'))
    const practice = await main.findByRole('link', { name: /^S’entraîner/ })
    expect(practice).toHaveAttribute('href', '/practice')
    // La révision intensive n'existe pas encore : la tuile ne l'annonce pas.
    expect(practice).toHaveTextContent('Un examen blanc, et tes erreurs à refaire')
    expect(main.getByRole('link', { name: /^Jouer/ })).toHaveAttribute('href', '/play')
  })

  it('keeps the quest of the day in a compact card, the other quests one click away', async () => {
    stubHome()
    renderAt('/', signedIn())

    // La première quête du jour qui reste à faire.
    const quest = within(await screen.findByRole('region', { name: 'Quête du jour' }))
    expect(quest.getByText('Réviser 3 cartes de code')).toBeInTheDocument()
    expect(quest.getByText('+25 XP')).toBeInTheDocument()
    expect(quest.getByRole('img', { name: 'Réviser 3 cartes de code : 1 / 3' })).toBeInTheDocument()
    expect(quest.getByRole('link', { name: 'Toutes tes quêtes' })).toHaveAttribute(
      'href',
      '/progress',
    )
    expect(screen.queryByRole('region', { name: 'Quêtes' })).not.toBeInTheDocument()
  })

  it('says so when every quest of the day is done', async () => {
    const done = QUESTS.daily.map((quest) => ({ ...quest, completed: true }))
    stubHome(stats(), { '/api/v1/me/quests': { body: { ...QUESTS, daily: done } } })
    renderAt('/', signedIn())

    const quest = await screen.findByRole('region', { name: 'Quête du jour' })
    expect(quest).toHaveTextContent('Tes quêtes du jour sont bouclées.')
  })

  it('leaves the goal ring, the calendar and the progress by subject to « Ta progression »', async () => {
    stubHome()
    renderAt('/', signedIn())

    await screen.findByRole('region', { name: 'Les collections' })
    expect(screen.queryByRole('img', { name: /^Objectif du jour/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Tes dernières semaines' })).not.toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Progression par sujet' })).not.toBeInTheDocument()
    // La série et l'XP de l'en-tête y mènent.
    expect(screen.getByRole('link', { name: 'Série : 3 jours' })).toHaveAttribute(
      'href',
      '/progress',
    )
    expect(screen.getByRole('link', { name: '120 XP' })).toHaveAttribute('href', '/progress')
  })

  it('shows a newcomer no block at zero: no due card, no progress before the first card', async () => {
    const java = subjectStats('java', 'Java', { counts: counts({ total: 30, seen: 0 }) })
    stubHome(stats({ overall: counts({ total: 30, seen: 0 }), subjects: [java] }), {
      [TOUR_PROGRESS]: { body: NEVER_OPENED },
    })
    renderAt('/', signedIn())

    expect(await screen.findByRole('heading', { name: 'Bienvenue Alice' })).toBeInTheDocument()
    expect(screen.getByText('Ta première session dure dix minutes. On y va ?')).toBeInTheDocument()
    const learn = await screen.findByRole('region', { name: 'Premiers pas' })
    expect(within(learn).getByRole('link', { name: 'Commencer' })).toBeInTheDocument()
    expect(within(learn).queryByRole('img')).not.toBeInTheDocument()
    expect(learn).not.toHaveTextContent('0 sur')
    expect(screen.queryByRole('link', { name: /à revoir/ })).not.toBeInTheDocument()
  })

  it('offers to retry when the statistics do not load', async () => {
    stubHome(stats(), { '/api/v1/me/review-stats': { status: 500, body: { status: 500 } } })
    renderAt('/', signedIn())

    expect(await screen.findByText('Impossible de charger tes statistiques.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Réessayer' })).toBeInTheDocument()
  })
})
