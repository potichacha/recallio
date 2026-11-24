import type { QueuedCard } from '../api/review'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'

/**
 * D'où vient la carte (ADR 0058) : son sujet et son thème dans une pastille, puis sa notion, qui dit de quoi parle la
 * question bien mieux que le thème, un domaine entier ; son format, discret, en bout de ligne. Une carte d'un document
 * Markdown n'a pas de notion : son thème la situe seul.
 */
export function CardHeader({ card }: { card: QueuedCard }) {
  return (
    <header className="flex flex-wrap items-center gap-x-2 gap-y-1.5 text-[0.8125rem] font-extrabold">
      <span className="rounded-full bg-brand-soft px-2.5 py-1.5 text-brand-strong">
        {card.subjectTitle} · {card.themeTitle}
      </span>
      {card.notionTitle && <span className="text-ink-muted">{card.notionTitle}</span>}
      <span className="ml-auto text-ink-muted">{CARD_TYPE_LABELS[card.type]}</span>
    </header>
  )
}
