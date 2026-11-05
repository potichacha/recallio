import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/errors'
import { useRecordReview, type QueuedCard, type ReviewResult } from '../api/review'
import { Markdown } from '../components/Markdown'
import { numberBlanks } from './blanks'
import { CardHeader } from './CardHeader'

/**
 * Un code à trous : on remplit chaque trou, on valide, et le serveur corrige trou par trou (ADR 0027). Comme pour un
 * QCM, la note n'est pas choisie ; la correction reste affichée jusqu'à ce qu'on passe à la suite.
 */
export function FillInCard({
  card,
  onReviewed,
}: {
  card: QueuedCard
  onReviewed: (result: ReviewResult) => void
}) {
  const [shownAt] = useState(() => Date.now())
  const [requestId] = useState(() => crypto.randomUUID())
  const [filled, setFilled] = useState<string[]>(() => Array(card.blankCount).fill(''))
  const record = useRecordReview()
  const result = record.data
  const blanks = result?.correction?.blanks

  const fill = (index: number, value: string) =>
    setFilled((current) => current.map((previous, other) => (other === index ? value : previous)))

  const submit = (event?: FormEvent) => {
    event?.preventDefault()
    record.mutate({
      requestId,
      cardId: card.cardId,
      filledBlanks: filled,
      elapsedMs: Date.now() - shownAt,
    })
  }

  return (
    <article className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <CardHeader card={card} />
      <Markdown>{numberBlanks(card.prompt)}</Markdown>
      <form onSubmit={submit} className="flex flex-col gap-3">
        {filled.map((value, index) => (
          <label key={index} className="flex flex-col gap-1">
            <span className="text-sm font-medium">Trou {index + 1}</span>
            <input
              value={value}
              onChange={(event) => fill(index, event.target.value)}
              readOnly={result !== undefined}
              spellCheck={false}
              autoCapitalize="off"
              autoComplete="off"
              className="rounded-lg border border-slate-300 px-3 py-2 font-mono text-sm"
            />
            {blanks?.[index] && (
              <span
                className={`text-sm ${blanks[index].correct ? 'text-emerald-700' : 'text-rose-700'}`}
              >
                {blanks[index].correct ? 'Juste' : `Attendu : ${blanks[index].expected}`}
              </span>
            )}
          </label>
        ))}
        {!result && (
          <button
            type="submit"
            disabled={filled.some((value) => value.trim() === '') || record.isPending}
            className="self-start rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            Valider
          </button>
        )}
      </form>

      {record.isError && (
        <div role="alert" className="flex items-center gap-3 text-sm text-rose-700">
          <span>
            {record.error instanceof ApiError
              ? record.error.message
              : "La réponse n'a pas pu être enregistrée."}
          </span>
          <button type="button" onClick={() => submit()} className="font-medium underline">
            Réessayer
          </button>
        </div>
      )}

      {result?.correction && (
        <div className="flex flex-col gap-3 border-t border-slate-100 pt-4">
          <p
            role="status"
            className={`font-medium ${result.rating === 'GOOD' ? 'text-emerald-700' : 'text-rose-700'}`}
          >
            {result.rating === 'GOOD'
              ? 'Tous les trous sont justes !'
              : 'Pas tout à fait : la réponse attendue est indiquée sous chaque trou faux.'}
          </p>
          <Markdown>{result.correction.explanation}</Markdown>
          <button
            type="button"
            onClick={() => onReviewed(result)}
            className="self-start rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700"
          >
            Carte suivante
          </button>
        </div>
      )}
    </article>
  )
}
