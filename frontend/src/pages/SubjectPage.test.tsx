import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderAt, stubApi } from '../test/render'

const GO = {
  id: 's1',
  slug: 'go',
  title: 'Go',
  category: 'LANGUAGES',
  position: 20,
  status: 'PUBLISHED',
  themeCount: 2,
  cardCount: 3,
}

function theme(slug: string, title: string, cardCount: number) {
  return { id: slug, subjectSlug: 'go', subjectTitle: 'Go', slug, title, position: 1, cardCount }
}

describe('SubjectPage', () => {
  it('lists the themes of the subject, each linking to its cards', async () => {
    stubApi({
      '/api/v1/subjects/go': { body: GO },
      '/api/v1/subjects/go/themes': {
        body: [
          theme('go-fondamentaux', 'GO : FONDAMENTAUX', 2),
          theme('go-concurrence', 'CONCURRENCE', 1),
        ],
      },
    })

    renderAt('/subjects/go')

    expect(await screen.findByRole('heading', { name: 'Go' })).toBeInTheDocument()
    expect(screen.getByText('Langages')).toBeInTheDocument()
    const link = await screen.findByRole('link', { name: /GO : FONDAMENTAUX/ })
    expect(link).toHaveAttribute('href', '/themes/go-fondamentaux')
    expect(link).toHaveTextContent('2 cartes')
    expect(screen.getByRole('link', { name: /CONCURRENCE/ })).toHaveTextContent('1 carte')
    expect(screen.getByRole('link', { name: 'Réviser ce sujet' })).toHaveAttribute(
      'href',
      '/review?subject=go',
    )
  })

  it('shows a not-found message for an unknown or unpublished subject', async () => {
    const notFound = { status: 404, body: { status: 404, code: 'content.subject-not-found' } }
    stubApi({ '/api/v1/subjects/inconnu': notFound, '/api/v1/subjects/inconnu/themes': notFound })

    renderAt('/subjects/inconnu')

    expect(await screen.findByRole('heading', { name: 'Sujet introuvable' })).toBeInTheDocument()
  })
})
