import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/errors'
import { useRecordReview, type QueuedCard, type ReviewResult } from '../api/review'
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
    setSelected((current) => {
      if (!card.multipleAnswers) return [index]
      return current.includes(index)
        ? current.filter((other) => other !== index)
        : [...current, index]
    })

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
              outcome={result ? outcome(result, index, selected.includes(index)) : undefined}
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

type Outcome = 'correct' | 'missed' | 'wrong' | 'neutral'

/** Ce qu'il faut montrer pour un choix, une fois la réponse corrigée. */
function outcome(result: ReviewResult, index: number, chosen: boolean): Outcome {
  const correct = result.correction?.correctChoices.includes(index) ?? false
  if (correct) return chosen ? 'correct' : 'missed'
  return chosen ? 'wrong' : 'neutral'
}

const OUTCOME_STYLES: Record<Outcome, string> = {
  correct: 'border-emerald-300 bg-emerald-50',
  missed: 'border-emerald-300 bg-white',
  wrong: 'border-rose-300 bg-rose-50',
  neutral: 'border-slate-200 bg-white',
}

const OUTCOME_LABELS: Record<Outcome, string | undefined> = {
  correct: 'Bonne réponse',
  missed: 'Bonne réponse, non cochée',
  wrong: 'Faux',
  neutral: undefined,
}

function ChoiceOption({
  text,
  type,
  checked,
  onChange,
  outcome,
}: {
  text: string
  type: 'radio' | 'checkbox'
  checked: boolean
  onChange: () => void
  outcome?: Outcome
}) {
  const style = outcome
    ? OUTCOME_STYLES[outcome]
    : 'border-slate-200 bg-white hover:border-indigo-300'
  const label = outcome && OUTCOME_LABELS[outcome]
  return (
    <label className={`flex items-start gap-3 rounded-lg border px-3 py-2 ${style}`}>
      <input
        type={type}
        name="choice"
        checked={checked}
        onChange={onChange}
        disabled={outcome !== undefined}
        className="mt-1"
      />
      <span className="flex flex-1 flex-col">
        <Markdown>{text}</Markdown>
        {label && <span className="text-xs font-medium text-slate-600">{label}</span>}
      </span>
    </label>
  )
}
