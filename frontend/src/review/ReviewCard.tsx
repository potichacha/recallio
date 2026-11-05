import { useState } from 'react'
import { ApiError } from '../api/errors'
import { useRecordReview, type QueuedCard, type Rating, type ReviewResult } from '../api/review'
import { AnswerDraft, DraftEcho } from '../components/AnswerDraft'
import { GRADED_BY_CHOICES } from '../components/cardTypeLabels'
import { FlipCard } from '../components/FlipCard'
import { Markdown } from '../components/Markdown'
import { CardHeader } from './CardHeader'
import { ExerciseCard } from './ExerciseCard'
import { FillInCard } from './FillInCard'
import { LessonCard } from './LessonCard'
import { MultipleChoiceCard } from './MultipleChoiceCard'

const FACE = 'flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm'

/** Les deux réponses d'une carte à auto-évaluer (ADR 0048) : les notes « À revoir » et « Bien » de FSRS. */
const SELF_ASSESSMENTS: { rating: Rating; label: string; style: string }[] = [
  {
    rating: 'AGAIN',
    label: 'Je ne savais pas',
    style: 'border-rose-200 text-rose-700 hover:bg-rose-50',
  },
  {
    rating: 'GOOD',
    label: 'Je savais',
    style: 'border-emerald-200 text-emerald-700 hover:bg-emerald-50',
  },
]

/**
 * Une carte à réviser : un QCM, un vrai ou faux ou un code à trous est corrigé par le serveur, un exercice de code
 * jugé par ses tests ; les autres formats s'auto-évaluent. La leçon de la notion, s'il faut la lire, passe avant.
 */
export function ReviewCard({
  card,
  onReviewed,
}: {
  card: QueuedCard
  onReviewed: (result: ReviewResult) => void
}) {
  const [lessonRead, setLessonRead] = useState(false)
  if (card.lesson && !lessonRead) {
    return <LessonCard card={card} onRead={() => setLessonRead(true)} />
  }
  if (GRADED_BY_CHOICES.has(card.type)) {
    return <MultipleChoiceCard card={card} onReviewed={onReviewed} />
  }
  if (card.type === 'FILL_IN') {
    return <FillInCard card={card} onReviewed={onReviewed} />
  }
  if (card.type === 'CODING_EXERCISE') {
    return <ExerciseCard card={card} onReviewed={onReviewed} />
  }
  return <SelfAssessedCard card={card} onReviewed={onReviewed} />
}

/**
 * Une carte en auto-évaluation (CLAUDE.md, section 6.1) : au recto la question, avec de quoi écrire sa réponse ; on la
 * retourne, puis on dit si on savait (ADR 0048).
 */
function SelfAssessedCard({
  card,
  onReviewed,
}: {
  card: QueuedCard
  onReviewed: (result: ReviewResult) => void
}) {
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
    <FlipCard
      flipped={revealed}
      front={
        <article className={FACE}>
          <CardHeader card={card} />
          <Markdown>{card.prompt}</Markdown>
          <AnswerDraft type={card.type} value={draft} onChange={setDraft} />
          <button
            type="button"
            onClick={() => setRevealed(true)}
            className="rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700"
          >
            Voir la réponse
          </button>
        </article>
      }
      back={
        <article className={FACE}>
          <CardHeader card={card} />
          <div className="text-slate-600">
            <Markdown>{card.prompt}</Markdown>
          </div>
          <DraftEcho type={card.type} value={draft} />
          <section aria-label="Réponse attendue" className="border-t border-slate-100 pt-4">
            <Markdown>{card.answer ?? ''}</Markdown>
          </section>
          <SelfAssessmentButtons disabled={chosen !== undefined} onRate={rate} />
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
        </article>
      }
    />
  )
}

function SelfAssessmentButtons({
  disabled,
  onRate,
}: {
  disabled: boolean
  onRate: (rating: Rating) => void
}) {
  return (
    <div role="group" aria-label="Connaissiez-vous la réponse ?" className="grid grid-cols-2 gap-2">
      {SELF_ASSESSMENTS.map(({ rating, label, style }) => (
        <button
          key={rating}
          type="button"
          disabled={disabled}
          onClick={() => onRate(rating)}
          className={`rounded-lg border px-3 py-3 font-medium disabled:opacity-50 ${style}`}
        >
          {label}
        </button>
      ))}
    </div>
  )
}
