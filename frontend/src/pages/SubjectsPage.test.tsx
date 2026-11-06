import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderAt, stubApi } from '../test/render'

function subject(overrides: Record<string, unknown>) {
  return {
    id: String(overrides.slug),
    title: String(overrides.slug),
    category: 'LANGUAGES',
    position: 10,
    status: 'PUBLISHED',
    themeCount: 2,
    cardCount: 10,
    ...overrides,
  }
}

describe('SubjectsPage', () => {
  it('groups the subjects by category, in the catalogue order', async () => {
    stubApi({
      '/api/v1/subjects': {
        body: [
          subject({ slug: 'java', title: 'Java', themeCount: 43, cardCount: 1311 }),
          subject({ slug: 'kubernetes', title: 'Kubernetes', category: 'CLOUD_DEVOPS' }),
          subject({ slug: 'go', title: 'Go', themeCount: 1, cardCount: 1 }),
        ],
      },
    })

    renderAt('/subjects')

    const languages = await screen.findByRole('region', { name: 'Langages' })
    const links = within(languages).getAllByRole('link')
    expect(links.map((link) => link.textContent)).toEqual([
      'Java43 thèmes · 1311 cartes',
      'Go1 thème · 1 carte',
    ])
    expect(links[0]).toHaveAttribute('href', '/subjects/java')
    expect(screen.getByRole('region', { name: 'Cloud & DevOps' })).toHaveTextContent('Kubernetes')
    expect(screen.queryByRole('region', { name: 'Frameworks' })).not.toBeInTheDocument()
  })

  it('marks the draft subjects that admins can proofread', async () => {
    stubApi({ '/api/v1/subjects': { body: [subject({ slug: 'go', status: 'DRAFT' })] } })

    renderAt('/subjects')

    expect(await screen.findByRole('link', { name: /go/ })).toHaveTextContent('Brouillon')
  })

  it('shows an error when the subjects cannot be loaded', async () => {
    stubApi({ '/api/v1/subjects': { status: 500, body: {} } })

    renderAt('/subjects')

    expect(await screen.findByRole('alert')).toHaveTextContent('Impossible de charger les sujets.')
  })
})
