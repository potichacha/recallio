import type { QueuedCard } from '../api/review'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'

/** Le format de la carte, et d'où elle vient. */
export function CardHeader({ card }: { card: QueuedCard }) {
  return (
    <header className="flex items-center justify-between gap-2 text-xs font-medium tracking-wide uppercase">
      <span className="text-brand">{CARD_TYPE_LABELS[card.type]}</span>
      <span className="text-right text-ink-faint">
        {card.subjectTitle} · {card.themeTitle}
      </span>
    </header>
  )
}
