import { useState } from 'react'
import type { Card } from '../api/content'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'
import { Markdown } from '../components/Markdown'
import { SourceFiles } from '../components/SourceFiles'
import { numberBlanks } from '../review/blanks'

/** Une carte en consultation : la réponse reste masquée jusqu'à ce qu'on choisisse de la révéler. */
export function CardItem({ card }: { card: Card }) {
  const [revealed, setRevealed] = useState(false)

  return (
    <article className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <span className="text-xs font-medium tracking-wide text-indigo-600 uppercase">
        {CARD_TYPE_LABELS[card.type]}
      </span>
      <Markdown>{card.blanks.length > 0 ? numberBlanks(card.prompt) : card.prompt}</Markdown>
      {card.exercise && (
        <>
          <SourceFiles title="Code de départ" files={card.exercise.starter} />
          <SourceFiles title="Tests" files={card.exercise.tests} />
        </>
      )}
      {card.choices.length > 0 && (
        <ul className="flex flex-col gap-1">
          {card.choices.map((choice, index) => (
            <li
              key={index}
              className={`rounded-lg border px-3 py-1.5 text-sm ${
                revealed && choice.correct ? 'border-emerald-300 bg-emerald-50' : 'border-slate-200'
              }`}
            >
              <Markdown>{choice.text}</Markdown>
              {revealed && choice.correct && <span className="sr-only">Bonne réponse</span>}
            </li>
          ))}
        </ul>
      )}
      {revealed ? (
        <div className="flex flex-col gap-2 border-t border-slate-100 pt-3">
          {card.blanks.length > 0 && (
            <ol className="flex flex-col gap-1 text-sm">
              {card.blanks.map((accepted, index) => (
                <li key={index}>
                  Trou {index + 1} : <code className="font-mono">{accepted.join(' ou ')}</code>
                </li>
              ))}
            </ol>
          )}
          {card.exercise && <SourceFiles title="Solution" files={card.exercise.solution} />}
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
