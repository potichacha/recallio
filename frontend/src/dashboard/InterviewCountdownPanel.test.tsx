import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { InterviewCountdown, InterviewStatus } from '../api/interviews'
import type { ReviewStats } from '../api/stats'
import { dayFromToday } from '../components/interviewDate'
import {
  fakeSession,
  renderAt,
  sentRequests,
  SIGNED_IN_API,
  stubApi,
  TEST_PROFILE,
  TEST_SUBJECTS,
} from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const counts = { total: 30, seen: 12, mastered: 4, dueNow: 0, dueToday: 0 }

/** Java suivi, déjà commencé, avec six notions essentielles : il a sa jauge, donc son compte à rebours. */
const STATS: ReviewStats = {
  overall: counts,
  newToday: 3,
  subjects: [
    {
      slug: 'java',
      title: 'Java',
      counts,
      newToday: 3,
      notions: { total: 12, discovered: 5, acquired: 3, mastered: 1, expert: 0 },
      readiness: { essentials: 6, acquired: 3, mastered: 1, percent: 33, ready: false },
      themes: [{ slug: 'bases', title: 'Bases', counts }],
    },
  ],
}

const QUESTS = { daily: [], dailyEndsOn: '2026-11-21', weekly: [], weeklyEndsOn: '2026-11-22' }

function countdown(overrides: Partial<InterviewCountdown> = {}): InterviewCountdown {
  return {
    subjectSlug: 'java',
    subjectTitle: 'Java',
    date: '2026-12-15',
    daysLeft: 23,
    status: 'ON_TRACK',
    readinessPercent: 33,
    week: 1,
    weeks: 4,
    reviewWeek: false,
    thisWeek: {
      startsOn: '2026-11-22',
      endsOn: '2026-11-28',
      quota: 2,
      notions: [
        { slug: 'equals-hashcode', title: 'equals et hashCode', mastery: 'DISCOVERED' },
        { slug: 'immutabilite', title: 'L’immutabilité', mastery: 'NEW' },
      ],
    },
    nextWeeks: [
      { number: 2, startsOn: '2026-11-29', endsOn: '2026-12-05', notions: 2, review: false },
      { number: 3, startsOn: '2026-12-06', endsOn: '2026-12-12', notions: 2, review: false },
      { number: 4, startsOn: '2026-12-13', endsOn: '2026-12-14', notions: 0, review: true },
    ],
    essentialsLeft: 6,
    essentials: 6,
    ...overrides,
  }
}

function stubDashboard(
  interviews: InterviewCountdown[],
  extra: Record<string, { status?: number; body: unknown }> = {},
) {
  return stubApi({
    ...SIGNED_IN_API,
    '/api/v1/subjects': { body: TEST_SUBJECTS },
    '/api/v1/me/review-stats': { body: STATS },
    '/api/v1/me/progress/activity': { body: [] },
    '/api/v1/me/quests': { body: QUESTS },
    '/api/v1/me/tour/java': { body: { validatedNotions: [], lastPage: null } },
    '/api/v1/me/interviews': { body: interviews },
    ...extra,
  })
}

/** Le bloc « Entretien » du sujet Java, sur le tableau de bord. */
const interviewOfJava = () => screen.findByRole('group', { name: 'Entretien Java' })

describe('InterviewCountdownPanel', () => {
  it('offers to give the date of an interview, then sends it', async () => {
    const date = dayFromToday(21)
    const fetchMock = stubDashboard([], {
      'PUT /api/v1/me/interviews/java': { body: countdown({ date }) },
    })
    renderAt('/', signedIn())

    const panel = within(await interviewOfJava())
    await userEvent.click(panel.getByRole('button', { name: 'Tu as un entretien ? Donne sa date' }))
    const form = panel.getByRole('form', { name: 'Date de ton entretien en Java' })
    const input = within(form).getByLabelText('Date de ton entretien')
    expect(input).toHaveAttribute('min', dayFromToday(1))
    expect(input).toHaveAttribute('max', dayFromToday(365))
    await userEvent.type(input, date)
    await userEvent.click(within(form).getByRole('button', { name: 'Valider' }))

    await waitFor(() => expect(form).not.toBeInTheDocument())
    const put = sentRequests(fetchMock).find((request) => request.method === 'PUT')
    expect(put?.url).toMatch(/\/api\/v1\/me\/interviews\/java$/)
    expect(await put?.json()).toEqual({ date })
  })

  it('gives the date with the keyboard alone', async () => {
    const date = dayFromToday(30)
    const fetchMock = stubDashboard([], {
      'PUT /api/v1/me/interviews/java': { body: countdown({ date }) },
    })
    renderAt('/', signedIn())

    const panel = within(await interviewOfJava())
    panel.getByRole('button', { name: 'Tu as un entretien ? Donne sa date' }).focus()
    await userEvent.keyboard('{Enter}')
    // Le champ prend le focus dès qu'il apparaît ; Entrée y envoie le formulaire.
    const input = panel.getByLabelText('Date de ton entretien')
    expect(input).toHaveFocus()
    await userEvent.type(input, `${date}{Enter}`)

    await waitFor(() =>
      expect(sentRequests(fetchMock).some((request) => request.method === 'PUT')).toBe(true),
    )
  })

  it('says why the server refuses a date', async () => {
    stubDashboard([], {
      'PUT /api/v1/me/interviews/java': {
        status: 422,
        body: {
          status: 422,
          code: 'interview.invalid-date',
          detail: 'Choisis une date après aujourd’hui, dans les 365 prochains jours.',
        },
      },
    })
    renderAt('/', signedIn())

    const panel = within(await interviewOfJava())
    await userEvent.click(panel.getByRole('button', { name: 'Tu as un entretien ? Donne sa date' }))
    await userEvent.type(panel.getByLabelText('Date de ton entretien'), dayFromToday(10))
    await userEvent.click(panel.getByRole('button', { name: 'Valider' }))

    expect(await panel.findByRole('alert')).toHaveTextContent(
      'Choisis une date après aujourd’hui, dans les 365 prochains jours.',
    )
  })

  it('shows the countdown, the status, and the notions of the week with their lesson', async () => {
    stubDashboard([countdown()])
    renderAt('/', signedIn())

    const panel = within(await interviewOfJava())
    expect(panel.getByText('Entretien dans 23 jours')).toBeInTheDocument()
    expect(panel.getByText('Le mardi 15 décembre 2026')).toBeInTheDocument()
    expect(panel.getByText('Dans les temps')).toBeInTheDocument()
    expect(panel.getByText('Cette semaine : 2 notions')).toBeInTheDocument()
    expect(panel.getByText('Semaine 1 sur 4 · du 22 novembre au 28 novembre')).toBeInTheDocument()
    const notions = within(panel.getByRole('list', { name: 'Les notions de la semaine' }))
    const items = notions.getAllByRole('listitem')
    expect(items).toHaveLength(2)
    expect(notions.getByRole('link', { name: 'equals et hashCode' })).toHaveAttribute(
      'href',
      '/subjects/java/course/equals-hashcode',
    )
    expect(items[0]).toHaveTextContent('Découverte')
    expect(items[1]).toHaveTextContent('Nouvelle')
    // Le parcours est « Préparer un entretien » : rien à signaler.
    expect(panel.queryByRole('link', { name: 'Changer de parcours' })).not.toBeInTheDocument()
  })

  it('folds the next weeks, the last one kept for reviewing', async () => {
    stubDashboard([countdown()])
    renderAt('/', signedIn())

    const panel = within(await interviewOfJava())
    const summary = panel.getByText('Les semaines suivantes')
    const lastWeek = panel.getByText('Semaine 4 · du 13 décembre au 14 décembre')
    expect(lastWeek).not.toBeVisible()
    expect(lastWeek.parentElement).toHaveTextContent('révision, sans nouvelle notion')

    await userEvent.click(summary)

    expect(lastWeek).toBeVisible()
    expect(
      panel.getByText('Semaine 2 · du 29 novembre au 5 décembre').parentElement,
    ).toHaveTextContent('2 notions')
  })

  it.each<[InterviewStatus, string]>([
    ['AHEAD', 'En avance'],
    ['ON_TRACK', 'Dans les temps'],
    ['BEHIND', 'En retard'],
    ['READY', 'Prêt'],
  ])(
    'says the status %s with an icon and a word, never the colour alone',
    async (status, label) => {
      stubDashboard([countdown({ status })])
      renderAt('/', signedIn())

      const panel = within(await interviewOfJava())
      const badge = panel.getByText(label, { exact: true })
      expect(badge.querySelector('svg')).not.toBeNull()
    },
  )

  it('says when the interview is today', async () => {
    stubDashboard([countdown({ daysLeft: 0 })])
    renderAt('/', signedIn())

    expect(within(await interviewOfJava()).getByText('C’est aujourd’hui')).toBeInTheDocument()
  })

  it('offers only to remove the date of an interview that is passed', async () => {
    stubDashboard([countdown({ status: 'PASSED', daysLeft: 0 })])
    renderAt('/', signedIn())

    const panel = within(await interviewOfJava())
    expect(panel.getByText('Ton entretien est passé')).toBeInTheDocument()
    expect(panel.getByRole('button', { name: 'Retirer la date' })).toBeInTheDocument()
    expect(panel.queryByRole('button', { name: 'Changer la date' })).not.toBeInTheDocument()
    expect(panel.queryByText(/Cette semaine/)).not.toBeInTheDocument()
  })

  it('asks for no new notion during the review week, only what is left to catch up', async () => {
    const reviewWeek = countdown({
      week: 4,
      reviewWeek: true,
      status: 'BEHIND',
      nextWeeks: [],
      thisWeek: { ...countdown().thisWeek, quota: 2 },
    })
    stubDashboard([reviewWeek])
    renderAt('/', signedIn())

    const panel = within(await interviewOfJava())
    expect(panel.getByText('Dernière semaine : révise, sans nouvelle notion')).toBeInTheDocument()
    expect(panel.getByText('Encore 2 notions à rattraper avant l’entretien :')).toBeInTheDocument()
    expect(panel.getAllByRole('link', { name: /equals et hashCode|L’immutabilité/ })).toHaveLength(
      2,
    )
    expect(panel.queryByText('Les semaines suivantes')).not.toBeInTheDocument()
  })

  it('changes the date, from the current one', async () => {
    const later = dayFromToday(40)
    const fetchMock = stubDashboard([countdown({ date: dayFromToday(20) })], {
      'PUT /api/v1/me/interviews/java': { body: countdown({ date: later }) },
    })
    renderAt('/', signedIn())

    const panel = within(await interviewOfJava())
    await userEvent.click(panel.getByRole('button', { name: 'Changer la date' }))
    const input = panel.getByLabelText('Date de ton entretien')
    expect(input).toHaveValue(dayFromToday(20))
    await userEvent.clear(input)
    await userEvent.type(input, later)
    await userEvent.click(panel.getByRole('button', { name: 'Valider' }))

    await waitFor(() =>
      expect(sentRequests(fetchMock).some((request) => request.method === 'PUT')).toBe(true),
    )
    const put = sentRequests(fetchMock).find((request) => request.method === 'PUT')
    expect(await put?.json()).toEqual({ date: later })
  })

  it('removes the date', async () => {
    const fetchMock = stubDashboard([countdown()], {
      'DELETE /api/v1/me/interviews/java': { status: 204, body: null },
    })
    renderAt('/', signedIn())

    const panel = within(await interviewOfJava())
    await userEvent.click(panel.getByRole('button', { name: 'Retirer la date' }))

    await waitFor(() =>
      expect(sentRequests(fetchMock).map((request) => request.method)).toContain('DELETE'),
    )
  })

  it('says when the track of the subject does not serve the essential notions first', async () => {
    stubDashboard([countdown()], {
      '/api/v1/me': { body: { ...TEST_PROFILE, tracks: { java: 'LEARN' } } },
    })
    renderAt('/', signedIn())

    const panel = within(await interviewOfJava())
    expect(
      await panel.findByText(/Ton parcours n’est pas « Préparer un entretien »/),
    ).toBeInTheDocument()
    expect(panel.getByRole('link', { name: 'Changer de parcours' })).toHaveAttribute(
      'href',
      '/profile/subjects',
    )
  })

  it('shows no countdown for a subject without essential notions', async () => {
    const markdown = {
      ...STATS.subjects[0],
      readiness: { essentials: 0, acquired: 0, mastered: 0, percent: 0, ready: false },
    }
    const fetchMock = stubDashboard([], {
      '/api/v1/me/review-stats': { body: { ...STATS, subjects: [markdown] } },
    })
    renderAt('/', signedIn())

    expect(await screen.findByRole('region', { name: 'Progression par sujet' })).toBeInTheDocument()
    expect(screen.queryByRole('group', { name: 'Entretien Java' })).not.toBeInTheDocument()
    expect(
      sentRequests(fetchMock).some((request) => request.url.endsWith('/api/v1/me/interviews')),
    ).toBe(false)
  })
})
