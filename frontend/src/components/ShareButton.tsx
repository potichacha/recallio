import { useState } from 'react'
import { shareCardImage, shareText, type ShareCard } from './shareCard'
import { button } from './ui/button'

type Outcome = 'shared' | 'downloaded' | 'copied' | 'failed'

const OUTCOMES: Record<Outcome, string> = {
  shared: 'Partagé !',
  downloaded: 'Image téléchargée : à vous de la publier.',
  copied: 'Texte copié : à vous de le coller.',
  failed: 'Le partage n’a pas fonctionné.',
}

/**
 * Partage une carte (ADR 0045) : par le partage du téléphone, avec l'image quand il l'accepte ; sinon l'image est
 * téléchargée, ou, faute d'image, le texte copié.
 */
export function ShareButton({ card, label = 'Partager' }: { card: ShareCard; label?: string }) {
  const [outcome, setOutcome] = useState<Outcome>()
  const [busy, setBusy] = useState(false)

  const share = async () => {
    setBusy(true)
    try {
      setOutcome(await shareCard(card))
    } catch (error) {
      // Fermer la fenêtre de partage n'est pas un échec.
      setOutcome(
        error instanceof DOMException && error.name === 'AbortError' ? undefined : 'failed',
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <span className="inline-flex flex-wrap items-center gap-2">
      <button
        type="button"
        onClick={share}
        disabled={busy}
        className={button({ variant: 'secondary', size: 'sm' })}
      >
        {label}
      </button>
      {outcome && (
        <span role="status" className="text-xs text-ink-muted">
          {OUTCOMES[outcome]}
        </span>
      )}
    </span>
  )
}

async function shareCard(card: ShareCard): Promise<Outcome> {
  const image = await shareCardImage(card)
  const text = { title: card.title, text: shareText(card), url: card.url }
  if (image) {
    const withImage = { ...text, files: [new File([image], 'recallio.png', { type: 'image/png' })] }
    if (navigator.canShare?.(withImage)) {
      await navigator.share(withImage)
      return 'shared'
    }
    download(image)
    return 'downloaded'
  }
  if (typeof navigator.share === 'function') {
    await navigator.share(text)
    return 'shared'
  }
  await navigator.clipboard.writeText(`${shareText(card)} ${card.url}`)
  return 'copied'
}

function download(image: Blob) {
  const url = URL.createObjectURL(image)
  const link = document.createElement('a')
  link.href = url
  link.download = 'recallio.png'
  link.click()
  URL.revokeObjectURL(url)
}
