import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { AdminContentReport } from '../api/reports'
import {
  fakeSession,
  renderAt,
  sentRequests,
  SIGNED_IN_API,
  stubApi,
  TEST_PROFILE,
} from '../test/render'

const ADMIN = { ...TEST_PROFILE, roles: ['admin'] }
const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

function report(overrides: Partial<AdminContentReport> = {}): AdminContentReport {
  return {
    id: 'r1',
    subject: 'go',
    cardId: null,
    themeSlug: null,
    notion: 'hello-world',
    page: 2,
    notionTitle: 'Le premier programme',
    excerpt: null,
    reason: 'UNCLEAR',
    comment: 'Le mot « paquet » n’est pas expliqué.',
    createdAt: '2026-11-04T09:30:00Z',
    resolution: null,
    resolvedAt: null,
    ...overrides,
  }
}

const page = (items: AdminContentReport[]) => ({
  items,
  page: 0,
  size: 20,
  totalItems: items.length,
  totalPages: 1,
})

describe('ContentReportsPage', () => {
  it('lists the open reports with a link to the reported content, and resolves one', async () => {
    const card = report({
      id: 'r2',
      cardId: 'c1',
      themeSlug: 'java-collections',
      notion: null,
      page: null,
      notionTitle: 'ArrayList',
      excerpt: 'Quelle est la complexité de get ?',
      reason: 'WRONG_ANSWER',
      comment: '',
    })
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      '/api/v1/me': { body: ADMIN },
      '/api/v1/admin/content-reports': { body: page([report(), card]) },
      'PUT /api/v1/admin/content-reports/r2/resolution': {
        body: { ...card, resolution: 'FIXED', resolvedAt: '2026-11-05T08:00:00Z' },
      },
    })
    renderAt('/admin/reports', signedIn())

    const [pageReport, cardReport] = await screen.findAllByRole('listitem')
    expect(pageReport).toHaveTextContent('Ce n’est pas clair')
    expect(within(pageReport).getByRole('link')).toHaveAttribute(
      'href',
      '/subjects/go/tour/hello-world?page=2',
    )
    expect(pageReport).toHaveTextContent('« Le mot « paquet » n’est pas expliqué. »')
    expect(cardReport).toHaveTextContent('Quelle est la complexité de get ?')
    expect(within(cardReport).getByRole('link')).toHaveAttribute('href', '/themes/java-collections')

    await userEvent.click(within(cardReport).getByRole('button', { name: 'Corrigé' }))

    const put = sentRequests(fetchMock).find((request) => request.method === 'PUT')
    expect(await put?.json()).toEqual({ resolution: 'FIXED' })
  })

  it('is kept to admins', async () => {
    stubApi({ ...SIGNED_IN_API, '/api/v1/admin/content-reports': { status: 403, body: {} } })
    renderAt('/admin/reports', signedIn())

    expect(await screen.findByText('Cette page est réservée aux admins.')).toBeInTheDocument()
  })
})
