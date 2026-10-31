import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
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

function stats(overrides: Partial<ReviewStats> = {}): ReviewStats {
  return {
    overall: counts({ total: 30, seen: 12, mastered: 4, dueNow: 5, dueToday: 7 }),
    newToday: 8,
    themes: [
      {
        subjectSlug: 'java',
        subjectTitle: 'Java',
        slug: 'collections',
        title: 'Collections',
        counts: counts({ total: 10, seen: 0 }),
      },
      {
        subjectSlug: 'java',
        subjectTitle: 'Java',
        slug: 'java-fondamentaux',
        title: 'Java : fondamentaux',
        counts: counts({ total: 20, seen: 12, mastered: 4, dueNow: 5 }),
      },
    ],
    ...overrides,
  }
}

const WEEK: DayActivity[] = [
  { day: '2026-10-23', cardsReviewed: 0, goalReached: false },
  { day: '2026-10-24', cardsReviewed: 3, goalReached: false },
  { day: '2026-10-25', cardsReviewed: 10, goalReached: true },
  { day: '2026-10-26', cardsReviewed: 12, goalReached: true },
  { day: '2026-10-27', cardsReviewed: 10, goalReached: true },
  { day: '2026-10-28', cardsReviewed: 11, goalReached: true },
  { day: '2026-10-29', cardsReviewed: 4, goalReached: false },
]

function stubDashboard(reviewStats: ReviewStats = stats()) {
  return stubApi({
    ...SIGNED_IN_API,
    '/api/v1/me/review-stats': { body: reviewStats },
    '/api/v1/me/progress/activity': { body: WEEK },
  })
}

describe('DashboardPage', () => {
  it('replaces the landing page once signed in', async () => {
    stubDashboard()
    renderAt('/', signedIn())

    expect(await screen.findByRole('heading', { name: 'Bonjour Alice' })).toBeInTheDocument()
    expect(screen.queryByText('Parcourir les thèmes')).not.toBeInTheDocument()
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

  it('shows the streak, the points and the mastered cards', async () => {
    stubDashboard()
    renderAt('/', signedIn())

    const streak = await screen.findByText('Série')
    expect(streak.nextElementSibling).toHaveTextContent('🔥 3 jours')
    expect(screen.getByText('Points').nextElementSibling).toHaveTextContent('120')
    expect(screen.getByText('Maîtrisées').nextElementSibling).toHaveTextContent('4 / 30')
  })

  it('shows the last seven days', async () => {
    stubDashboard()
    renderAt('/', signedIn())

    expect(
      await screen.findByLabelText('dimanche 25 octobre 2026 : objectif atteint'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('jeudi 29 octobre 2026 : 4 carte(s)')).toBeInTheDocument()
  })

  it('lists started themes first, with a shortcut to review their due cards', async () => {
    stubDashboard()
    renderAt('/', signedIn())

    const section = await screen.findByRole('region', { name: 'Progression par thème' })
    const themes = within(section).getAllByRole('listitem')
    expect(themes[0]).toHaveTextContent('Java : fondamentaux')
    expect(themes[0]).toHaveTextContent('12 / 20 vues · 4 maîtrisées')
    expect(within(themes[0]).getByRole('link', { name: '5 à revoir' })).toHaveAttribute(
      'href',
      '/review?theme=java-fondamentaux',
    )
    expect(themes[1]).toHaveTextContent('Collections')
  })
})
