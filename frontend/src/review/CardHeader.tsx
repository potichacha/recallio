import type { QueuedCard } from '../api/review'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'

/**
 * Le format de la carte et d'où elle vient (ADR 0058) : son sujet et son thème, puis sa notion en titre, qui dit de
 * quoi parle la question bien mieux que le thème, un domaine entier. Une carte d'un document Markdown n'a pas de
 * notion : son thème la situe seul.
 */
export function CardHeader({ card }: { card: QueuedCard }) {
  return (
    <header className="flex flex-col gap-1">
      <p className="flex items-center justify-between gap-2 text-sm">
        <span className="font-bold text-brand">{CARD_TYPE_LABELS[card.type]}</span>
        <span className="text-right text-ink-muted">
          {card.subjectTitle} · {card.themeTitle}
        </span>
      </p>
      {card.notionTitle && <p className="text-lg font-extrabold">{card.notionTitle}</p>}
    </header>
  )
}
