import type { QueuedCard } from '../api/review'
import { Markdown } from '../components/Markdown'
import { CardHeader } from './CardHeader'
import { button } from '../components/ui/button'

/**
 * La leçon d'une notion, avant sa toute première carte en parcours « apprendre » (ADR 0032) : on la lit, puis on passe
 * à la carte. Elle ne compte pas comme une révision.
 */
export function LessonCard({ card, onRead }: { card: QueuedCard; onRead: () => void }) {
  return (
    <article
      aria-label={`Leçon : ${card.notionTitle ?? card.themeTitle}`}
      className="flex flex-col gap-4 rounded-xl border border-brand-line bg-brand-soft/40 p-5 shadow-sm"
    >
      <CardHeader card={card} />
      <h2 className="text-lg font-semibold">Leçon : {card.notionTitle}</h2>
      <Markdown>{card.lesson ?? ''}</Markdown>
      <button type="button" onClick={onRead} className={`self-start ${button()}`}>
        Passer à la carte
      </button>
    </article>
  )
}
