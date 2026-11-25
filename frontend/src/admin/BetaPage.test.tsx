import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, SIGNED_IN_API, stubApi, TEST_PROFILE } from '../test/render'
import { USAGE_FEATURE_LABELS } from './usageFeatureLabels'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const ADMIN_API = {
  ...SIGNED_IN_API,
  '/api/v1/me': { body: { ...TEST_PROFILE, roles: ['admin'] } },
}

const QUIET_DAY = {
  reviewers: 0,
  reviews: 0,
  goalsReached: 0,
  accountsCreated: 0,
  tourValidations: 0,
}

/** Trente jours, du 27 octobre au 25 novembre 2026 ; `busy` donne l'activité de certains jours, par leur rang. */
const days = (busy: Record<number, Partial<typeof QUIET_DAY>> = {}) =>
  Array.from({ length: 30 }, (_, index) => ({
    day: new Date(Date.UTC(2026, 9, 27 + index)).toISOString().slice(0, 10),
    ...QUIET_DAY,
    ...busy[index],
  }))

const feature = (name: string, accounts: number, percent: number | null, dated = true) => ({
  feature: name,
  accounts,
  percent,
  dated,
})

const FEATURE_NAMES = Object.keys(USAGE_FEATURE_LABELS)
const UNDATED = ['INTERVIEW_COUNTDOWN', 'REMINDERS', 'PUBLIC_PROFILE', 'GROUP']

const CARD = {
  cardId: 'c1',
  subject: 'java',
  subjectTitle: 'Java',
  themeSlug: 'java-objet',
  notionTitle: 'equals et hashCode',
  excerpt: 'Deux objets égaux ont-ils le même hashCode ?',
  type: 'TRUE_FALSE',
}

const REPORT = {
  from: '2026-10-27',
  to: '2026-11-25',
  days: days({
    28: { reviewers: 3, reviews: 40, goalsReached: 1, accountsCreated: 2, tourValidations: 4 },
    29: { reviewers: 2, reviews: 1210, goalsReached: 2, accountsCreated: 1, tourValidations: 0 },
  }),
  subjects: [
    {
      subject: 'java',
      title: 'Java',
      followers: 5,
      reviewers: 3,
      reviews: 1200,
      successPercent: 71,
    },
    { subject: 'go', title: 'Go', followers: 2, reviewers: 0, reviews: 0, successPercent: null },
    // Un sujet dépublié depuis : il n'a plus que son slug.
    { subject: 'docker', title: null, followers: 1, reviewers: 1, reviews: 50, successPercent: 40 },
  ],
  failedCards: [
    { card: CARD, answers: 8, failures: 6, failurePercent: 75, openReports: 2 },
    {
      card: { cardId: 'c2', subject: 'docker', subjectTitle: null, type: null },
      answers: 5,
      failures: 3,
      failurePercent: 60,
      openReports: 0,
    },
  ],
  reportedCards: [{ card: CARD, openReports: 2 }],
  tour: {
    learners: 2,
    stops: [
      {
        subject: 'go',
        title: 'Go',
        chapters: [
          { domainTitle: 'Bases', chapterTitle: 'Premiers pas', accounts: 3 },
          { domainTitle: 'Bases', chapterTitle: 'Types', accounts: 1 },
          { domainTitle: 'Concurrence', chapterTitle: 'Goroutines', accounts: 0 },
        ],
      },
    ],
  },
  features: {
    activeAccounts: 4,
    features: FEATURE_NAMES.map((name) =>
      name === 'REVIEW'
        ? feature(name, 3, 75)
        : name === 'REMINDERS'
          ? feature(name, 1, 25, false)
          : feature(name, 0, 0, !UNDATED.includes(name)),
    ),
  },
}

/** Une bêta qui démarre : les sujets publiés sont là, tout le reste est à zéro. */
const EMPTY_REPORT = {
  ...REPORT,
  days: days(),
  subjects: [
    {
      subject: 'java',
      title: 'Java',
      followers: 0,
      reviewers: 0,
      reviews: 0,
      successPercent: null,
    },
  ],
  failedCards: [],
  reportedCards: [],
  tour: { learners: 0, stops: [] },
  features: {
    activeAccounts: 0,
    features: FEATURE_NAMES.map((name) => feature(name, 0, null, !UNDATED.includes(name))),
  },
}

const cells = (row: HTMLElement) =>
  within(row)
    .getAllByRole('cell')
    .map((cell) => cell.textContent?.replace(/\s/g, ' '))

function open(report: unknown = REPORT) {
  stubApi({ ...ADMIN_API, '/api/v1/admin/beta': { body: report } })
  renderAt('/admin/beta', signedIn())
}

describe('BetaPage', () => {
  it('sums the thirty days in four figures, and sends to the retention page', async () => {
    open()

    expect(await screen.findByRole('heading', { level: 1, name: 'Bêta' })).toBeInTheDocument()
    expect(await screen.findByText(/^Du 27 oct\. au 25 nov\., en UTC\./)).toBeInTheDocument()
    const figure = (label: string) =>
      screen
        .getByText(label, { selector: 'dt' })
        .nextElementSibling?.textContent?.replace(/\s/g, ' ')
    expect(figure('Comptes actifs')).toBe('4')
    expect(figure('Révisions')).toBe('1 250')
    expect(figure('Comptes créés')).toBe('3')
    expect(figure('Notions validées dans le Tour')).toBe('4')
    expect(screen.getByRole('link', { name: 'Rétention et parcours' })).toHaveAttribute(
      'href',
      '/admin/retention',
    )
    expect(document.title).toBe('Bêta · Recallio')
  })

  it('draws the accounts that reviewed each day, then the series that the admin picks', async () => {
    open()

    const chart = await screen.findByRole('img', { name: 'Comptes qui ont révisé, par jour' })
    expect(chart).toHaveAccessibleDescription(
      /Du 27 oct\. au 25 nov\. : 5 au total, le plus haut le 24 nov\. avec 3\./,
    )
    expect(chart.querySelectorAll('rect')).toHaveLength(30)
    const series = screen.getByRole('group', { name: 'Série affichée' })
    expect(within(series).getAllByRole('radio')).toHaveLength(5)
    expect(within(series).getByRole('radio', { name: 'Comptes qui ont révisé' })).toBeChecked()

    await userEvent.click(within(series).getByRole('radio', { name: 'Révisions' }))

    const reviews = screen.getByRole('img', { name: 'Révisions, par jour' })
    expect(reviews).toHaveAccessibleDescription(/1\s250 au total, le plus haut le 25 nov\./)
    const table = screen.getByRole('table', { name: 'Révisions, par jour', hidden: true })
    const rows = within(table).getAllByRole('row', { hidden: true })
    expect(rows).toHaveLength(1 + 30)
    expect(rows[30].textContent?.replace(/\s/g, ' ')).toBe('25 nov.1 210')
    expect(
      screen.getByText('Les cartes révisées, au jour de chacun dans son fuseau.'),
    ).toBeVisible()
  })

  it('lists the subjects, with a dash where there is no answer to rate', async () => {
    open()

    const table = await screen.findByRole('table', { name: 'Sujets' })
    expect(
      within(table)
        .getAllByRole('columnheader')
        .map((header) => header.textContent),
    ).toEqual(['Sujet', 'Suivis', 'Comptes qui révisent', 'Réponses', 'Réussite'])
    const [, java, go, docker] = within(table).getAllByRole('row')
    expect(within(java).getByRole('rowheader')).toHaveTextContent('Java')
    expect(cells(java)).toEqual(['5', '3', '1 200', '71 %'])
    expect(cells(go)).toEqual(['2', '0', '0', '–'])
    expect(within(docker).getByRole('rowheader')).toHaveTextContent('docker')
  })

  it('shows the most failed cards with their notion, their format and their failure rate', async () => {
    open()

    const table = await screen.findByRole('table', { name: 'Cartes les plus ratées' })
    const [, first, retired] = within(table).getAllByRole('row')
    const card = within(first).getByRole('rowheader')
    expect(card).toHaveTextContent('Java · equals et hashCode · Vrai ou faux')
    expect(
      within(card).getByRole('link', { name: 'Deux objets égaux ont-ils le même hashCode ?' }),
    ).toHaveAttribute('href', '/themes/java-objet')
    expect(cells(first)).toEqual(['8', '75 %6 sur 8', '2'])
    // Une carte retirée, ou d'un sujet dépublié : il ne reste que son sujet.
    expect(within(retired).getByRole('rowheader')).toHaveTextContent(
      'docker · carte retirée ou sujet dépublié',
    )
    expect(within(retired).queryByRole('link')).not.toBeInTheDocument()
  })

  it('shows the most reported cards, and leads to the reports to handle', async () => {
    open()

    const table = await screen.findByRole('table', { name: 'Cartes les plus signalées' })
    expect(cells(within(table).getAllByRole('row')[1])).toEqual(['2'])
    expect(screen.getByRole('link', { name: 'signalements à traiter' })).toHaveAttribute(
      'href',
      '/admin/reports',
    )
  })

  it('says where each account stands in the Tour, chapter by chapter, not who gave up', async () => {
    open()

    const tour = (await screen.findByRole('heading', { name: 'Le Tour' })).closest('section')
    expect(tour).toHaveTextContent('2 comptes ont validé au moins une notion du Tour')
    expect(tour).toHaveTextContent('Ce n’est pas un abandon')
    const chapters = within(tour as HTMLElement).getByRole('list', {
      name: 'Go : comptes par chapitre du Tour',
    })
    expect(
      within(chapters)
        .getAllByRole('listitem')
        .map((item) => item.textContent),
    ).toEqual([
      'Premiers pas · Bases3 comptes',
      'Types · Bases1 compte',
      'Goroutines · Concurrence0 compte',
    ])
  })

  it('names every feature in French, with its accounts, its share, and « à ce jour » when undated', async () => {
    open()

    const list = await screen.findByRole('list', {
      name: 'Part des comptes actifs par fonctionnalité',
    })
    const items = within(list)
      .getAllByRole('listitem')
      .map((item) => item.textContent)
    expect(items).toHaveLength(14)
    expect(items[0]).toBe('Révision3 comptes · 75 %')
    expect(items).toContain('Rappels · à ce jour1 compte · 25 %')
    expect(items).toContain('Examen blanc0 compte · 0 %')
    expect(items.filter((item) => item?.includes('à ce jour'))).toHaveLength(4)
    expect(items.join(' ')).not.toMatch(/undefined|[A-Z]{2,}_/)
    expect(screen.getByText(/La part des 4 comptes actifs/)).toBeInTheDocument()
  })

  it('says what to expect when the beta has just started, rather than drawing empty charts', async () => {
    open(EMPTY_REPORT)

    expect(await screen.findByText(/Personne n’a encore rien fait sur cette période/)).toBeVisible()
    expect(
      screen.getByText('Comptes actifs', { selector: 'dt' }).nextElementSibling,
    ).toHaveTextContent('0')
    expect(screen.getByText('Rien sur ces trente jours.')).toBeInTheDocument()
    expect(screen.getByText('Aucune carte assez jouée.')).toBeInTheDocument()
    expect(screen.getByText('Aucun signalement ouvert.')).toBeInTheDocument()
    expect(screen.getByText('Personne n’a encore ouvert le Tour.')).toBeInTheDocument()
    expect(screen.getByText('Aucun compte actif sur ces trente jours.')).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    // Les sujets publiés restent listés, à zéro et sans taux.
    const [, java] = within(screen.getByRole('table', { name: 'Sujets' })).getAllByRole('row')
    expect(cells(java)).toEqual(['0', '0', '0', '–'])
    // On peut encore choisir une série : chacune dit qu'elle n'a rien.
    await userEvent.click(screen.getByRole('radio', { name: 'Comptes créés' }))
    expect(screen.getByText(/« Comptes créés » : une barre par jour/)).toBeInTheDocument()
  })

  it('offers to retry when the report fails to load', async () => {
    const responses: Parameters<typeof stubApi>[0] = {
      ...ADMIN_API,
      '/api/v1/admin/beta': { status: 500, body: { status: 500, title: 'Internal Server Error' } },
    }
    const fetchMock = stubApi(responses)
    renderAt('/admin/beta', signedIn())

    const failure = await screen.findByRole('alert')
    expect(failure).toHaveTextContent('Impossible de charger les chiffres de la bêta.')
    expect(screen.getByRole('heading', { level: 1, name: 'Bêta' })).toBeInTheDocument()

    responses['/api/v1/admin/beta'] = { body: REPORT }
    await userEvent.click(within(failure).getByRole('button', { name: 'Réessayer' }))

    expect(await screen.findByRole('table', { name: 'Sujets' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    await waitFor(() =>
      expect(
        fetchMock.mock.calls.filter(([request]) => request.url.endsWith('/api/v1/admin/beta')),
      ).toHaveLength(2),
    )
  })

  it('is reserved to the admins', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/admin/beta': {
        status: 403,
        body: { status: 403, title: 'Forbidden', detail: 'Réservé aux admins' },
      },
    })
    renderAt('/admin/beta', signedIn())

    expect(
      await screen.findByText('Cette page est réservée aux administrateurs.'),
    ).toBeInTheDocument()
  })
})
