import { useState } from 'react'
import type { Card } from '../api/content'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'
import { Markdown } from '../components/Markdown'

/** Une carte en consultation : la réponse reste masquée jusqu'à ce qu'on choisisse de la révéler. */
export function CardItem({ card }: { card: Card }) {
  const [revealed, setRevealed] = useState(false)

  return (
    <article className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <span className="text-xs font-medium tracking-wide text-indigo-600 uppercase">
        {CARD_TYPE_LABELS[card.type]}
      </span>
      <Markdown>{card.prompt}</Markdown>
      {revealed ? (
        <div className="border-t border-slate-100 pt-3">
          <Markdown>{card.answer}</Markdown>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => setRevealed(true)}
          className="self-start rounded-lg border border-indigo-200 px-3 py-1.5 text-sm font-medium text-indigo-700 hover:bg-indigo-50"
        >
          Voir la réponse
        </button>
      )}
    </article>
  )
}
