import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ShareButton } from './ShareButton'
import { shareText, type ShareCard } from './shareCard'

const CARD: ShareCard = {
  kicker: 'Carte des compétences',
  title: 'Chapitre maîtrisé',
  detail: 'Concurrence',
  url: 'https://recallio.example/subjects/go',
}

describe('ShareButton', () => {
  beforeEach(() => {
    // jsdom ne dessine pas : sans image, le partage se rabat sur le texte.
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('shares the text of the card through the phone when it cannot draw the image', async () => {
    const share = vi.fn().mockResolvedValue(undefined)
    vi.stubGlobal('navigator', { ...navigator, share })
    render(<ShareButton card={CARD} />)

    await userEvent.click(screen.getByRole('button', { name: 'Partager' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Partagé !')
    expect(share).toHaveBeenCalledWith({
      title: 'Chapitre maîtrisé',
      text: 'Chapitre maîtrisé : Concurrence. Carte des compétences sur Recallio.',
      url: CARD.url,
    })
  })

  it('copies the text when the browser cannot share', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    vi.stubGlobal('navigator', { clipboard: { writeText } })
    render(<ShareButton card={CARD} label="Partager mon podium" />)

    await userEvent.click(screen.getByRole('button', { name: 'Partager mon podium' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Texte copié')
    expect(writeText).toHaveBeenCalledWith(`${shareText(CARD)} ${CARD.url}`)
  })

  it('says nothing when the user closes the share sheet', async () => {
    const share = vi.fn().mockRejectedValue(new DOMException('fermé', 'AbortError'))
    vi.stubGlobal('navigator', { ...navigator, share })
    render(<ShareButton card={CARD} />)

    await userEvent.click(screen.getByRole('button', { name: 'Partager' }))

    expect(share).toHaveBeenCalled()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
})
