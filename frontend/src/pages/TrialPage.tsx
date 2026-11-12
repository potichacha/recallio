import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { useState, type FormEvent } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { useSubjects } from '../api/content'
import { ApiError } from '../api/errors'
import { useCheckTrialAnswer, useTrial, type TrialQuestion } from '../api/trial'
import { useSession } from '../auth/session'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'
import { GradedQuestion } from '../components/GradedQuestion'
import { emptyDraft, isDraftComplete, toGradedAnswer } from '../components/gradedAnswers'
import { button } from '../components/ui/button'
import { useFocusOnAppear } from '../components/useFocusOnAppear'
import { answerFeedback } from '../review/feedback'
import { trialStart } from './trialStart'

/**
 * L'essai sans compte (ADR 0050) : cinq questions corrigées tout de suite, puis l'invitation à créer son compte pour
 * garder sa progression. On voit ce que vaut l'application avant de donner son adresse.
 */
export function TrialPage() {
  const { slug = '' } = useParams()
  const trial = useTrial(slug)
  const subjects = useSubjects()
  const location = useLocation()
  // La première question a pu être posée sur la page publique : l'essai reprend alors à la suivante.
  const [start] = useState(() => trialStart(location.state))
  const [position, setPosition] = useState(start.answered)
  const [right, setRight] = useState(start.right)
  const title = subjects.data?.find((subject) => subject.slug === slug)?.title ?? slug

  if (trial.error instanceof ApiError && trial.error.status === 404) {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-bold">Pas encore d’essai pour ce sujet</h1>
        <Link to="/" className="text-brand hover:underline">
          Retour à l’accueil
        </Link>
      </section>
    )
  }
  if (trial.isError) {
    return (
      <ErrorState message="Impossible de charger l’essai." onRetry={() => void trial.refetch()} />
    )
  }
  if (!trial.data) return <LoadingState label="Préparation de l’essai…" shape="card" />

  const { questions } = trial.data
  const question = questions.at(position)
  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-wrap items-baseline justify-between gap-2">
        <h1 className="text-2xl font-bold">Essai : {title}</h1>
        {question && (
          <p className="text-sm text-ink-muted">
            Question {position + 1} sur {questions.length}
          </p>
        )}
      </header>
      {question ? (
        <TrialCard
          key={question.card.cardId}
          subject={slug}
          question={question}
          onNext={(correct) => {
            if (correct) setRight((current) => current + 1)
            setPosition((current) => current + 1)
          }}
        />
      ) : (
        <TrialSummary right={right} total={questions.length} />
      )}
    </section>
  )
}

function TrialCard({
  subject,
  question,
  onNext,
}: {
  subject: string
  question: TrialQuestion
  onNext: (correct: boolean) => void
}) {
  const { card, notion } = question
  const check = useCheckTrialAnswer(subject, notion)
  const [draft, setDraft] = useState(() => emptyDraft(card))
  const correction = check.data
  const nextQuestion = useFocusOnAppear<HTMLButtonElement>(correction !== undefined)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    check.mutate(toGradedAnswer(card, draft), {
      onSuccess: (result) => answerFeedback(result.correct),
    })
  }

  return (
    <form
      onSubmit={submit}
      className="flex flex-col gap-4 rounded-2xl border border-line bg-surface p-5 shadow-sm"
    >
      <GradedQuestion
        question={card}
        label={CARD_TYPE_LABELS[card.type]}
        draft={draft}
        correction={correction}
        onChange={setDraft}
      />
      {correction ? (
        <button
          ref={nextQuestion}
          type="button"
          onClick={() => onNext(correction.correct)}
          className={`self-start ${button()}`}
        >
          Question suivante
        </button>
      ) : (
        <button
          type="submit"
          disabled={!isDraftComplete(card, draft) || check.isPending}
          className={`self-start ${button()}`}
        >
          {check.isPending ? 'Vérification…' : 'Vérifier'}
        </button>
      )}
      {check.error && (
        <p role="alert" className="text-sm text-danger">
          {check.error instanceof ApiError ? check.error.message : 'La vérification a échoué.'}
        </p>
      )}
    </form>
  )
}

/** Le bilan de l'essai, et l'invitation à créer son compte : la suite se garde, la série commence aujourd'hui. */
function TrialSummary({ right, total }: { right: number; total: number }) {
  const session = useSession()
  return (
    <section
      aria-label="Bilan de l’essai"
      className="flex flex-col items-start gap-4 rounded-2xl bg-surface p-5 shadow-sm"
    >
      <p className="text-3xl font-bold motion-safe:animate-pop">
        {right} / {total}
      </p>
      <h2 className="text-lg font-semibold">
        {right === total ? 'Sans faute !' : 'Bien joué, tu as fait le tour de l’essai.'}
      </h2>
      <p className="text-ink-soft">
        Crée ton compte pour garder ta progression : les questions ratées reviendront au bon moment,
        et ta série commence aujourd’hui.
      </p>
      <div className="flex flex-wrap gap-3">
        <button type="button" onClick={() => session.login('/welcome')} className={button()}>
          Créer mon compte
        </button>
        <Link to="/" className={button({ variant: 'secondary' })}>
          Essayer un autre sujet
        </Link>
      </div>
    </section>
  )
}
