import type { QueuedCard } from '../api/review'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'

/**
 * Le format de la carte, et d'où elle vient : son sujet, puis sa notion, qui dit de quoi parle la question bien mieux
 * que le thème, un domaine entier. Une carte d'un document Markdown n'a pas de notion : son thème la situe.
 */
export function CardHeader({ card }: { card: QueuedCard }) {
  return (
    <header className="flex items-center justify-between gap-2 text-sm">
      <span className="font-semibold text-brand">{CARD_TYPE_LABELS[card.type]}</span>
      <span className="text-right text-ink-muted">
        {card.subjectTitle} · {card.notionTitle ?? card.themeTitle}
      </span>
    </header>
  )
}
