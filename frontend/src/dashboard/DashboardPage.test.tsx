import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { QuestBoard } from '../api/quests'
import type { DayActivity, ReviewStats } from '../api/stats'
import { fakeSession, renderAt, SIGNED_IN_API, stubApi } from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const counts = (overrides: Partial<ReviewStats['overall']> = {}) => ({
  total: 0,
  seen: 0,
  mastered: 0,
  dueNow: 0,
  dueToday: 0,
  ...overrides,
})

const notions = (overrides: Partial<ReviewStats['subjects'][number]['notions']> = {}) => ({
  total: 0,
  discovered: 0,
  acquired: 0,
  mastered: 0,
  expert: 0,
  ...overrides,
})

const readiness = (overrides: Partial<ReviewStats['subjects'][number]['readiness']> = {}) => ({
  essentials: 0,
  acquired: 0,
  mastered: 0,
  percent: 0,
  ready: false,
  ...overrides,
})

function stats(overrides: Partial<ReviewStats> = {}): ReviewStats {
  return {
    overall: counts({ total: 30, seen: 12, mastered: 4, dueNow: 5, dueToday: 7 }),
    newToday: 8,
    subjects: [
      {
        slug: 'java',
        title: 'Java',
        counts: counts({ total: 30, seen: 12, mastered: 4, dueNow: 5 }),
        newToday: 8,
        notions: notions({ total: 12, discovered: 5, acquired: 3, mastered: 1 }),
        readiness: readiness({ essentials: 6, acquired: 3, mastered: 1, percent: 33 }),
        themes: [
          { slug: 'collections', title: 'Collections', counts: counts({ total: 10, seen: 0 }) },
          {
            slug: 'java-fondamentaux',
            title: 'Java : fondamentaux',
            counts: counts({ total: 20, seen: 12, mastered: 4, dueNow: 5 }),
          },
        ],
      },
    ],
    ...overrides,
  }
}

const WEEK: DayActivity[] = [
  { day: '2026-10-23', cardsReviewed: 10, goalReached: true, frozen: false },
  { day: '2026-10-24', cardsReviewed: 3, goalReached: false, frozen: true },
  { day: '2026-10-25', cardsReviewed: 10, goalReached: true, frozen: false },
  { day: '2026-10-26', cardsReviewed: 12, goalReached: true, frozen: false },
  { day: '2026-10-27', cardsReviewed: 10, goalReached: true, frozen: false },
  { day: '2026-10-28', cardsReviewed: 11, goalReached: true, frozen: false },
  { day: '2026-10-29', cardsReviewed: 4, goalReached: false, frozen: false },
]

const QUESTS: QuestBoard = {
  daily: [
    { quest: 'DAILY_REVIEWS', target: 15, progress: 15, xp: 20, completed: true },
    { quest: 'DAILY_CODE', target: 3, progress: 1, xp: 25, completed: false },
    { quest: 'DAILY_TOUR_NOTION', target: 1, progress: 0, xp: 20, completed: false },
  ],
  dailyEndsOn: '2026-10-30',
  weekly: [
    { quest: 'WEEKLY_DAYS', target: 5, progress: 4, xp: 60, completed: false },
    { quest: 'WEEKLY_FORMATS', target: 5, progress: 2, xp: 50, completed: false },
  ],
  weeklyEndsOn: '2026-11-02',
}

function stubDashboard(reviewStats: ReviewStats = stats()) {
  return stubApi({
    ...SIGNED_IN_API,
    '/api/v1/me/review-stats': { body: reviewStats },
    '/api/v1/me/progress/activity': { body: WEEK },
    '/api/v1/me/quests': { body: QUESTS },
  })
}

describe('DashboardPage', () => {
  it('replaces the landing page once signed in', async () => {
    stubDashboard()
    renderAt('/', signedIn())

    expect(await screen.findByRole('heading', { name: 'Bonjour Alice' })).toBeInTheDocument()
    expect(screen.queryByText('Parcourir les sujets')).not.toBeInTheDocument()
  })

  it('tells what to review today and links to the session', async () => {
    stubDashboard()
    renderAt('/', signedIn())

    expect(await screen.findByText('5 cartes à revoir, 8 nouvelles')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Réviser maintenant' })).toHaveAttribute(
      'href',
      '/review',
    )
  })

  it('says when everything is up to date for now', async () => {
    stubDashboard(stats({ overall: counts({ total: 30, seen: 30, dueToday: 2 }), newToday: 0 }))
    renderAt('/', signedIn())

    expect(
      await screen.findByText("Tout est à jour : encore 2 cartes à revoir plus tard aujourd'hui."),
    ).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Réviser maintenant' })).not.toBeInTheDocument()
  })

  it('shows the streak and its freezes, the XP and the mastered cards', async () => {
    stubDashboard()
    renderAt('/', signedIn())

    const streak = await screen.findByText('Série')
    expect(streak.nextElementSibling).toHaveTextContent('3 jours')
    expect(streak.parentElement).toHaveTextContent('1 gel · prochain dans 4 jours')
    expect(screen.getByText('XP').nextElementSibling).toHaveTextContent('120')
    expect(screen.getByText('Maîtrisées').nextElementSibling).toHaveTextContent('4 / 30')
  })

  it('shows the quests of the day and of the week, with their XP', async () => {
    stubDashboard()
    renderAt('/', signedIn())

    const section = await screen.findByRole('region', { name: 'Quêtes' })
    const today = within(section).getByRole('list', { name: 'Aujourd’hui' })
    const quests = within(today).getAllByRole('listitem')
    expect(quests).toHaveLength(3)
    expect(quests[0]).toHaveTextContent('Réviser 15 cartes')
    expect(quests[0]).toHaveTextContent('+20 XP')
    expect(
      within(today).getByRole('progressbar', { name: 'Réviser 3 cartes de code : 1 / 3' }),
    ).toBeInTheDocument()
    expect(quests[2]).toHaveTextContent('Valider une notion du Tour')
    const week = within(section).getByRole('list', { name: 'Cette semaine, jusqu’à dimanche' })
    expect(
      within(week).getByRole('progressbar', { name: 'Réviser 5 jours dans la semaine : 4 / 5' }),
    ).toBeInTheDocument()
  })

  it('shows the last seven days, with the missed days covered by a freeze', async () => {
    stubDashboard()
    renderAt('/', signedIn())

    expect(
      await screen.findByLabelText('dimanche 25 octobre 2026 : objectif atteint'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('jeudi 29 octobre 2026 : 4 carte(s)')).toBeInTheDocument()
    // Le jour couvert par un gel montre un flocon, et rien à lire : son nom accessible dit déjà tout.
    const frozen = screen.getByLabelText('samedi 24 octobre 2026 : jour manqué couvert par un gel')
    expect(frozen.querySelector('svg')).not.toBeNull()
  })

  it('shows the progress of each followed subject, with a shortcut to review its due cards', async () => {
    const go = {
      slug: 'go',
      title: 'Go',
      counts: counts({ total: 40, seen: 2 }),
      newToday: 20,
      notions: notions(),
      readiness: readiness(),
      themes: [
        {
          slug: 'go-fondamentaux',
          title: 'GO : FONDAMENTAUX',
          counts: counts({ total: 40, seen: 2 }),
        },
      ],
    }
    stubDashboard(stats({ subjects: [...stats().subjects, go] }))
    renderAt('/', signedIn())

    const section = await screen.findByRole('region', { name: 'Progression par sujet' })
    const java = within(section).getByRole('link', { name: 'Java' }).closest('li') as HTMLElement
    expect(java).toHaveTextContent('12 / 30 vues · 4 maîtrisées')
    expect(within(java).getAllByRole('link', { name: '5 à revoir' })[0]).toHaveAttribute(
      'href',
      '/review?subject=java',
    )
    const goRow = within(section).getByRole('link', { name: 'Go' }).closest('li') as HTMLElement
    expect(goRow).toHaveTextContent('2 / 40 vues · 0 maîtrisée')
    expect(within(goRow).queryByRole('link', { name: /à revoir/ })).not.toBeInTheDocument()
    // Plusieurs sujets : les thèmes sont repliés.
    expect(within(goRow).getByRole('list', { name: 'Thèmes Go', hidden: true })).not.toBeVisible()
  })

  it('lists the started themes of a subject first, with a shortcut to review their due cards', async () => {
    stubDashboard()
    renderAt('/', signedIn())

    const list = await screen.findByRole('list', { name: 'Thèmes Java' })
    const themes = within(list).getAllByRole('listitem')
    expect(themes[0]).toHaveTextContent('Java : fondamentaux')
    expect(themes[0]).toHaveTextContent('12 / 20 vues · 4 maîtrisées')
    expect(within(themes[0]).getByRole('link', { name: '5 à revoir' })).toHaveAttribute(
      'href',
      '/review?theme=java-fondamentaux',
    )
    expect(themes[1]).toHaveTextContent('Collections')
  })

  it('shows the level of each followed subject and the XP towards the next one', async () => {
    stubDashboard()
    renderAt('/', signedIn())

    const section = await screen.findByRole('region', { name: 'Progression par sujet' })
    const java = within(section).getByRole('link', { name: 'Java' }).closest('li') as HTMLElement
    expect(await within(java).findByText('Niveau 3')).toBeInTheDocument()
    expect(
      within(java).getByRole('progressbar', { name: 'Java : 40 / 300 XP vers le niveau 4' }),
    ).toBeInTheDocument()
    expect(java).toHaveTextContent('40 / 300 XP')
  })

  it('tells how ready a subject is for an interview, from its essential notions', async () => {
    const ready = readiness({ essentials: 6, acquired: 6, mastered: 4, percent: 83, ready: true })
    const go = { ...stats().subjects[0], slug: 'go', title: 'Go', readiness: ready }
    stubDashboard(stats({ subjects: [...stats().subjects, go] }))
    renderAt('/', signedIn())

    const section = await screen.findByRole('region', { name: 'Progression par sujet' })
    const java = within(section).getByRole('link', { name: 'Java' }).closest('li') as HTMLElement
    expect(
      within(java).getByRole('progressbar', { name: 'Java : prêt pour l’entretien à 33 %' }),
    ).toBeInTheDocument()
    expect(java).toHaveTextContent(
      '3 / 6 notions essentielles acquises, dont 1 maîtrisée : prêt à partir de 80 %.',
    )
    const goRow = within(section).getByRole('link', { name: 'Go' }).closest('li') as HTMLElement
    expect(goRow).toHaveTextContent('83 %')
    expect(goRow).toHaveTextContent('dont 4 maîtrisées : vous êtes prêt.')
  })

  it('sums up the mastery of the notions of a subject, with a link to its skill map', async () => {
    stubDashboard()
    renderAt('/', signedIn())

    const section = await screen.findByRole('region', { name: 'Progression par sujet' })
    const java = within(section).getByRole('link', { name: 'Java' }).closest('li') as HTMLElement
    expect(within(java).getByRole('img', { name: /^Notions Java/ })).toHaveAccessibleName(
      'Notions Java : 3 / 12 notions acquises · 1 maîtrisée · 0 experte',
    )
    expect(within(java).getByRole('link', { name: 'Carte des compétences' })).toHaveAttribute(
      'href',
      '/subjects/java/path',
    )
  })

  it('shows no skill map for a subject without referential', async () => {
    stubDashboard(
      stats({ subjects: [{ ...stats().subjects[0], notions: notions(), readiness: readiness() }] }),
    )
    renderAt('/', signedIn())

    const section = await screen.findByRole('region', { name: 'Progression par sujet' })
    const java = within(section).getByRole('link', { name: 'Java' }).closest('li') as HTMLElement
    expect(
      within(java).queryByRole('link', { name: 'Carte des compétences' }),
    ).not.toBeInTheDocument()
  })
})
