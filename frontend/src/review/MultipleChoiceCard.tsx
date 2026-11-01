import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/errors'
import { useRecordReview, type QueuedCard } from '../api/review'
import { ChoiceOption } from '../components/ChoiceOption'
import { choiceOutcome, toggleChoice } from '../components/choices'
import { Markdown } from '../components/Markdown'
import { CardHeader } from './CardHeader'

/**
 * Un QCM : on coche, on valide, et le serveur corrige (ADR 0019). La note n'est pas choisie : juste, la carte est
 * « retrouvée » ; fausse, elle est à revoir. La correction reste affichée jusqu'à ce qu'on passe à la suite.
 */
export function MultipleChoiceCard({
  card,
  onReviewed,
}: {
  card: QueuedCard
  onReviewed: () => void
}) {
  const [shownAt] = useState(() => Date.now())
  const [requestId] = useState(() => crypto.randomUUID())
  const [selected, setSelected] = useState<number[]>([])
  const record = useRecordReview()
  const result = record.data

  const toggle = (index: number) =>
    setSelected((current) => toggleChoice(current, index, card.multipleAnswers))

  const submit = (event?: FormEvent) => {
    event?.preventDefault()
    record.mutate({
      requestId,
      cardId: card.cardId,
      selectedChoices: [...selected].sort((a, b) => a - b),
      elapsedMs: Date.now() - shownAt,
    })
  }

  return (
    <article className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <CardHeader card={card} />
      <Markdown>{card.prompt}</Markdown>
      <form onSubmit={submit} className="flex flex-col gap-3">
        <fieldset className="flex flex-col gap-2">
          <legend className="mb-1 text-sm text-slate-500">
            {card.multipleAnswers ? 'Plusieurs réponses possibles' : 'Une seule réponse'}
          </legend>
          {card.choices.map((choice, index) => (
            <ChoiceOption
              key={index}
              text={choice}
              type={card.multipleAnswers ? 'checkbox' : 'radio'}
              checked={selected.includes(index)}
              onChange={() => toggle(index)}
              outcome={
                result?.correction
                  ? choiceOutcome(result.correction.correctChoices, index, selected.includes(index))
                  : undefined
              }
            />
          ))}
        </fieldset>
        {!result && (
          <button
            type="submit"
            disabled={selected.length === 0 || record.isPending}
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
              ? 'Bonne réponse !'
              : 'Pas tout à fait : les bonnes réponses sont marquées.'}
          </p>
          <Markdown>{result.correction.explanation}</Markdown>
          <button
            type="button"
            onClick={onReviewed}
            className="self-start rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700"
          >
            Carte suivante
          </button>
        </div>
      )}
    </article>
  )
}
