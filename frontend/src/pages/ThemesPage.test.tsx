import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderAt, stubApi } from '../test/render'

describe('ThemesPage', () => {
  it('lists the themes with their card count and links to each one', async () => {
    stubApi({
      '/api/v1/themes': {
        body: [
          {
            id: '1',
            slug: 'java-fondamentaux',
            title: 'JAVA : FONDAMENTAUX',
            position: 1,
            cardCount: 37,
          },
          { id: '2', slug: 'glossaire', title: 'GLOSSAIRE', position: 1000, cardCount: 1 },
        ],
      },
    })

    renderAt('/themes')

    const link = await screen.findByRole('link', { name: /JAVA : FONDAMENTAUX/ })
    expect(link).toHaveAttribute('href', '/themes/java-fondamentaux')
    expect(link).toHaveTextContent('37 cartes')
    expect(screen.getByRole('link', { name: /GLOSSAIRE/ })).toHaveTextContent('1 carte')
  })

  it('shows an error when the themes cannot be loaded', async () => {
    stubApi({ '/api/v1/themes': { status: 500, body: {} } })

    renderAt('/themes')

    expect(await screen.findByRole('alert')).toHaveTextContent('Impossible de charger les thèmes.')
  })
})
