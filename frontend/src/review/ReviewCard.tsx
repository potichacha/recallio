import { useState } from 'react'
import { ApiError } from '../api/errors'
import { useRecordReview, type QueuedCard, type Rating } from '../api/review'
import { GRADED_BY_CHOICES } from '../components/cardTypeLabels'
import { Markdown } from '../components/Markdown'
import { CardHeader } from './CardHeader'
import { formatDelay } from './formatDelay'
import { MultipleChoiceCard } from './MultipleChoiceCard'

const RATING_LABELS: Record<Rating, string> = {
  AGAIN: 'À revoir',
  HARD: 'Difficile',
  GOOD: 'Bien',
  EASY: 'Facile',
}

const RATING_STYLES: Record<Rating, string> = {
  AGAIN: 'border-rose-200 text-rose-700 hover:bg-rose-50',
  HARD: 'border-amber-200 text-amber-700 hover:bg-amber-50',
  GOOD: 'border-emerald-200 text-emerald-700 hover:bg-emerald-50',
  EASY: 'border-sky-200 text-sky-700 hover:bg-sky-50',
}

/**
 * Une carte à réviser : un QCM ou un vrai ou faux est corrigé par le serveur, les autres formats s'auto-évaluent.
 */
export function ReviewCard({ card, onReviewed }: { card: QueuedCard; onReviewed: () => void }) {
  return GRADED_BY_CHOICES.has(card.type) ? (
    <MultipleChoiceCard card={card} onReviewed={onReviewed} />
  ) : (
    <SelfAssessedCard card={card} onReviewed={onReviewed} />
  )
}

/**
 * Une carte en auto-évaluation (CLAUDE.md, section 6.1) : la question, puis la réponse, puis l'une des quatre notes.
 * Une mise en situation ou un bug à corriger propose d'abord d'écrire sa réponse, pour la comparer à la réponse modèle.
 */
function SelfAssessedCard({ card, onReviewed }: { card: QueuedCard; onReviewed: () => void }) {
  const [shownAt] = useState(() => Date.now())
  // Un identifiant par carte présentée : une nouvelle tentative après une erreur réseau renvoie le même.
  const [requestId] = useState(() => crypto.randomUUID())
  const [revealed, setRevealed] = useState(false)
  const [draft, setDraft] = useState('')
  const [chosen, setChosen] = useState<Rating>()
  const record = useRecordReview()

  const rate = (rating: Rating) => {
    setChosen(rating)
    record.mutate(
      { requestId, cardId: card.cardId, rating, elapsedMs: Date.now() - shownAt },
      { onSuccess: onReviewed },
    )
  }

  return (
    <article className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <CardHeader card={card} />
      <Markdown>{card.prompt}</Markdown>

      {(card.type === 'OPEN_SCENARIO' || card.type === 'BUG_FIX') && (
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">
            {card.type === 'BUG_FIX'
              ? 'Votre correction (facultatif)'
              : 'Votre réponse (facultatif)'}
          </span>
          <textarea
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            readOnly={revealed}
            rows={5}
            spellCheck={card.type !== 'BUG_FIX'}
            className={`rounded-lg border border-slate-300 px-3 py-2 ${card.type === 'BUG_FIX' ? 'font-mono text-sm' : ''}`}
          />
        </label>
      )}

      {revealed ? (
        <>
          <div className="border-t border-slate-100 pt-4">
            <Markdown>{card.answer ?? ''}</Markdown>
          </div>
          <RatingButtons
            card={card}
            shownAt={shownAt}
            disabled={chosen !== undefined}
            onRate={rate}
          />
          {record.isError && chosen && (
            <div role="alert" className="flex items-center gap-3 text-sm text-rose-700">
              <span>
                {record.error instanceof ApiError
                  ? record.error.message
                  : "La réponse n'a pas pu être enregistrée."}
              </span>
              <button type="button" onClick={() => rate(chosen)} className="font-medium underline">
                Réessayer
              </button>
            </div>
          )}
        </>
      ) : (
        <button
          type="button"
          onClick={() => setRevealed(true)}
          className="rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700"
        >
          Voir la réponse
        </button>
      )}
    </article>
  )
}

function RatingButtons({
  card,
  shownAt,
  disabled,
  onRate,
}: {
  card: QueuedCard
  shownAt: number
  disabled: boolean
  onRate: (rating: Rating) => void
}) {
  return (
    <div role="group" aria-label="Noter la carte" className="grid grid-cols-2 gap-2 sm:grid-cols-4">
      {card.previews.map(({ rating, dueAt }) => (
        <button
          key={rating}
          type="button"
          disabled={disabled}
          onClick={() => onRate(rating)}
          aria-label={RATING_LABELS[rating]}
          className={`flex flex-col items-center rounded-lg border px-3 py-2 font-medium disabled:opacity-50 ${RATING_STYLES[rating]}`}
        >
          {RATING_LABELS[rating]}
          <span className="text-xs font-normal opacity-75">
            {formatDelay(Date.parse(dueAt) - shownAt)}
          </span>
        </button>
      ))}
    </div>
  )
}
