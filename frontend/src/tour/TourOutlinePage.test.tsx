import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderAt, stubApi } from '../test/render'
import { TEST_TOUR } from './tourFixtures'

describe('TourOutlinePage', () => {
  it('lists the domains, chapters and notions of the Tour, with a link to start it', async () => {
    stubApi({ '/api/v1/subjects/go/tour': { body: TEST_TOUR } })

    renderAt('/subjects/go/tour')

    expect(await screen.findByRole('heading', { name: 'Le Tour de Go' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Commencer le Tour' })).toHaveAttribute(
      'href',
      '/subjects/go/tour/hello-world',
    )
    const domain = screen.getByRole('region', { name: 'Bases du langage' })
    const firstSteps = within(domain).getByRole('list', { name: 'Premiers pas' })
    expect(
      within(firstSteps)
        .getAllByRole('listitem')
        .map((item) => item.textContent),
    ).toEqual(['Le premier programme', 'Packages et imports · 2 pages'])
    expect(within(domain).getByRole('link', { name: 'La valeur zéro' })).toHaveAttribute(
      'href',
      '/subjects/go/tour/valeur-zero',
    )
  })

  it('says when a subject has no Tour yet', async () => {
    stubApi({ '/api/v1/subjects/go/tour': { body: { ...TEST_TOUR, domains: [] } } })

    renderAt('/subjects/go/tour')

    expect(await screen.findByText('Ce sujet n’a pas encore de Tour.')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Commencer le Tour' })).not.toBeInTheDocument()
  })

  it('shows a not-found message for an unknown or unpublished subject', async () => {
    stubApi({
      '/api/v1/subjects/inconnu/tour': {
        status: 404,
        body: { status: 404, code: 'content.subject-not-found' },
      },
    })

    renderAt('/subjects/inconnu/tour')

    expect(await screen.findByRole('heading', { name: 'Sujet introuvable' })).toBeInTheDocument()
  })
})
