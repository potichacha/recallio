import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderAt, stubApi } from '../test/render'

const THEME = {
  id: '1',
  subjectSlug: 'java',
  subjectTitle: 'Java',
  slug: 'java-fondamentaux',
  title: 'JAVA : FONDAMENTAUX',
  position: 1,
  cardCount: 1,
}
const CARD = {
  id: 'c1',
  type: 'CODE_OUTPUT',
  prompt: 'Que vaut `0.1 + 0.2 == 0.3` ?',
  answer: '**false** : imprécision des flottants.',
  choices: [],
  blanks: [],
  version: 1,
}

function stubTheme(cards: unknown[], totalPages = 1) {
  stubApi({
    '/api/v1/themes/java-fondamentaux': { body: THEME },
    '/api/v1/themes/java-fondamentaux/cards': {
      body: { items: cards, page: 0, size: 20, totalItems: cards.length, totalPages },
    },
  })
}

describe('ThemePage', () => {
  it('shows the cards of the theme with their type, answers hidden', async () => {
    stubTheme([CARD])

    renderAt('/themes/java-fondamentaux')

    expect(await screen.findByRole('heading', { name: 'JAVA : FONDAMENTAUX' })).toBeInTheDocument()
    expect(await screen.findByText("Qu'affiche ce code ?")).toBeInTheDocument()
    expect(screen.getByText('0.1 + 0.2 == 0.3')).toBeInTheDocument()
    expect(screen.queryByText(/imprécision des flottants/)).not.toBeInTheDocument()
  })

  it('reveals the answer, rendered as Markdown, on demand', async () => {
    stubTheme([CARD])
    renderAt('/themes/java-fondamentaux')

    await userEvent.click(await screen.findByRole('button', { name: 'Voir la réponse' }))

    expect(screen.getByText('false').tagName).toBe('STRONG')
    expect(screen.getByText(/imprécision des flottants/)).toBeInTheDocument()
  })

  it('links back to the themes of its subject', async () => {
    stubTheme([CARD])

    renderAt('/themes/java-fondamentaux')

    expect(await screen.findByRole('link', { name: '← Thèmes Java' })).toHaveAttribute(
      'href',
      '/subjects/java',
    )
  })

  it('highlights code blocks in the language announced by the document', async () => {
    stubTheme([{ ...CARD, prompt: '```go\nfunc main() {}\n```' }])

    renderAt('/themes/java-fondamentaux')

    const keyword = await screen.findByText('func')
    expect(keyword).toHaveClass('hljs-keyword')
    expect(keyword.closest('code')).toHaveClass('language-go')
  })

  it('shows the choices of a multiple-choice card, and marks the right one once revealed', async () => {
    stubTheme([
      {
        ...CARD,
        type: 'MULTIPLE_CHOICE',
        choices: [
          { text: 'Dans la pile', correct: false },
          { text: 'Dans le tas', correct: true },
        ],
      },
    ])
    renderAt('/themes/java-fondamentaux')

    expect(await screen.findByText('Dans la pile')).toBeInTheDocument()
    expect(screen.queryByText('Bonne réponse')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Voir la réponse' }))

    expect(screen.getByText('Dans le tas').closest('li')).toHaveTextContent('Bonne réponse')
    expect(screen.getByText('Dans la pile').closest('li')).not.toHaveTextContent('Bonne réponse')
  })

  it('shows the pagination only when there are several pages', async () => {
    stubTheme([CARD], 3)

    renderAt('/themes/java-fondamentaux')

    expect(await screen.findByText('Page 1 / 3')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Précédent' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Suivant' })).toBeEnabled()
  })

  it('shows a not-found message for an unknown theme', async () => {
    stubApi({
      '/api/v1/themes/inconnu': {
        status: 404,
        body: {
          status: 404,
          code: 'content.theme-not-found',
          title: 'Ressource introuvable',
          type: 'about:blank',
          detail: '…',
        },
      },
      '/api/v1/themes/inconnu/cards': {
        status: 404,
        body: { status: 404, code: 'content.theme-not-found' },
      },
    })

    renderAt('/themes/inconnu')

    expect(await screen.findByRole('heading', { name: 'Thème introuvable' })).toBeInTheDocument()
  })
})
