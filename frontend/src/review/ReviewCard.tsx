import { useEffect, useRef, useState } from 'react'
import { ApiError } from '../api/errors'
import { useRecordReview, type QueuedCard, type Rating, type ReviewResult } from '../api/review'
import { AnswerDraft, DraftEcho } from '../components/AnswerDraft'
import { FlipCard } from '../components/FlipCard'
import { Markdown } from '../components/Markdown'
import { CardHeader } from './CardHeader'
import { ExerciseCard } from './ExerciseCard'
import { FillInCard } from './FillInCard'
import { LessonCard } from './LessonCard'
import { MultipleChoiceCard } from './MultipleChoiceCard'
import { onControl, swipeRating, typingIn } from './swipe'
import { button } from '../components/ui/button'

const FACE = 'flex flex-col gap-4 rounded-xl border border-line bg-surface p-5 shadow-sm'

/** Les deux réponses d'une carte à auto-évaluer (ADR 0048) : les notes « À revoir » et « Bien » de FSRS. */
const SELF_ASSESSMENTS: { rating: Rating; label: string; style: string; shortcut: string }[] = [
  {
    rating: 'AGAIN',
    label: 'Je ne savais pas',
    style: 'border-danger-line text-danger hover:bg-danger-soft',
    shortcut: '1',
  },
  {
    rating: 'GOOD',
    label: 'Je savais',
    style: 'border-success-line text-success hover:bg-success-soft',
    shortcut: '2',
  },
]

/**
 * Une carte à réviser : une carte à choix (un QCM, un vrai ou faux, ou une carte d'un autre format qui en a reçu, ADR
 * 0051) ou un code à trous est corrigé par le serveur, un exercice de code jugé par ses tests ; une carte qui n'a pas
 * encore de choix s'auto-évalue. La leçon de la notion, s'il faut la lire, passe avant.
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
  if (card.choices.length > 0) {
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
 * retourne, puis on dit si on savait (ADR 0048). Au doigt, on glisse la carte ; au clavier, Espace la retourne, puis 1 ou
 * 2 répond (ADR 0050).
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
  const [drag, setDrag] = useState(0)
  const dragStart = useRef<number | null>(null)
  const record = useRecordReview()

  const rate = (rating: Rating) => {
    setChosen(rating)
    record.mutate(
      { requestId, cardId: card.cardId, rating, elapsedMs: Date.now() - shownAt },
      { onSuccess: onReviewed },
    )
  }

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.altKey || event.ctrlKey || event.metaKey || typingIn(event.target)) return
      if (!revealed && (event.key === ' ' || event.key === 'Enter') && !onControl(event.target)) {
        event.preventDefault()
        setRevealed(true)
      } else if (revealed && chosen === undefined && (event.key === '1' || event.key === '2')) {
        event.preventDefault()
        rate(event.key === '1' ? 'AGAIN' : 'GOOD')
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  })

  const endDrag = (answer: boolean) => {
    const rating = answer ? swipeRating(drag) : undefined
    dragStart.current = null
    setDrag(0)
    if (rating && chosen === undefined) rate(rating)
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
            aria-keyshortcuts="Space"
            className={button()}
          >
            Voir la réponse
          </button>
        </article>
      }
      back={
        <article
          className={`${FACE} touch-pan-y`}
          style={drag ? { transform: `translateX(${drag}px) rotate(${drag / 24}deg)` } : undefined}
          onPointerDown={(event) => {
            if (event.pointerType !== 'mouse' && chosen === undefined)
              dragStart.current = event.clientX
          }}
          onPointerMove={(event) => {
            if (dragStart.current !== null) setDrag(event.clientX - dragStart.current)
          }}
          onPointerUp={() => endDrag(true)}
          onPointerCancel={() => endDrag(false)}
        >
          <CardHeader card={card} />
          <div className="text-ink-muted">
            <Markdown>{card.prompt}</Markdown>
          </div>
          <DraftEcho type={card.type} value={draft} />
          <section aria-label="Réponse attendue" className="border-t border-line pt-4">
            <Markdown>{card.answer ?? ''}</Markdown>
          </section>
          <SelfAssessmentButtons disabled={chosen !== undefined} onRate={rate} />
          <p className="text-center text-xs text-ink-muted">
            Glisse la carte : à droite si tu savais, à gauche sinon. Au clavier : 1 ou 2.
          </p>
          {record.isError && chosen && (
            <div role="alert" className="flex items-center gap-3 text-sm text-danger">
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
    <div role="group" aria-label="Connaissais-tu la réponse ?" className="grid grid-cols-2 gap-2">
      {SELF_ASSESSMENTS.map(({ rating, label, style, shortcut }) => (
        <button
          key={rating}
          type="button"
          disabled={disabled}
          onClick={() => onRate(rating)}
          aria-keyshortcuts={shortcut}
          className={`min-h-12 rounded-lg border-2 px-3 py-3 font-semibold transition-transform active:translate-y-0.5 disabled:opacity-50 motion-reduce:transition-none ${style}`}
        >
          {label}
        </button>
      ))}
    </div>
  )
}
