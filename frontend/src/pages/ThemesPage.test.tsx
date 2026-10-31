import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderAt, stubApi } from '../test/render'

const theme = (id: string, subject: string, slug: string, title: string, cardCount: number) => ({
  id,
  subjectSlug: subject.toLowerCase(),
  subjectTitle: subject,
  slug,
  title,
  position: Number(id),
  cardCount,
})

describe('ThemesPage', () => {
  it('lists the themes of each subject with their card count and links to each one', async () => {
    stubApi({
      '/api/v1/themes': {
        body: [
          theme('1', 'Java', 'java-fondamentaux', 'JAVA : FONDAMENTAUX', 37),
          theme('2', 'Java', 'java-glossaire', 'GLOSSAIRE', 1),
          theme('3', 'Go', 'go-concurrence', 'CONCURRENCE', 12),
        ],
      },
    })

    renderAt('/themes')

    const java = await screen.findByRole('region', { name: 'Java' })
    const link = within(java).getByRole('link', { name: /JAVA : FONDAMENTAUX/ })
    expect(link).toHaveAttribute('href', '/themes/java-fondamentaux')
    expect(link).toHaveTextContent('37 cartes')
    expect(within(java).getByRole('link', { name: /GLOSSAIRE/ })).toHaveTextContent('1 carte')
    const go = screen.getByRole('region', { name: 'Go' })
    expect(within(go).getAllByRole('link')).toHaveLength(1)
  })

  it('shows an error when the themes cannot be loaded', async () => {
    stubApi({ '/api/v1/themes': { status: 500, body: {} } })

    renderAt('/themes')

    expect(await screen.findByRole('alert')).toHaveTextContent('Impossible de charger les thèmes.')
  })
})
