import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/errors'
import type { PracticeAnswer, PracticeCard, PracticeCorrection } from '../api/practice'
import { useRunExercise, type ExerciseFile } from '../api/review'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'
import { GradedQuestion } from '../components/GradedQuestion'
import {
  emptyDraft,
  isAnsweredOnServer,
  isDraftComplete,
  toGradedAnswer,
} from '../components/gradedAnswers'
import { LessonLink } from '../components/LessonLink'
import { Markdown } from '../components/Markdown'
import { ReportButton } from '../components/ReportButton'
import { ErrorState } from '../components/states/ErrorState'
import { useFocusOnAppear } from '../components/useFocusOnAppear'
import { ExerciseEditor } from '../review/ExerciseEditor'
import { ExerciseRunResult } from '../review/ExerciseRunResult'
import { button } from '../components/ui/button'

const CARD_STYLE = 'flex flex-col gap-4 rounded-xl border border-line bg-surface p-5 shadow-sm'
const PRIMARY = `self-start ${button()}`
const SECONDARY = `self-start ${button({ variant: 'secondary' })}`

/** Une carte passée : juste ou non, d'après le serveur ; un exercice est juste quand ses tests passent. */
type Outcome = boolean

/** Corrige une réponse sur le serveur, sans l'enregistrer : l'entraînement d'une notion et le carnet ont chacun la leur. */
type CheckAnswer = (answer: PracticeAnswer) => Promise<PracticeCorrection>

/**
 * Des cartes à passer une à une, sans rien enregistrer : l'entraînement d'une notion (ADR 0038) et le carnet d'erreurs
 * (ADR 0057). Le serveur corrige les cartes à choix et les codes à trous ; un exercice s'essaie contre ses tests. Il n'y
 * a pas d'autre carte (ADR 0063). À la fin, le bilan des réponses justes.
 */
export function PracticeDeck({
  subject,
  cards,
  check,
  startAt,
  showNotion = false,
  doneLabel,
}: {
  subject: string
  cards: PracticeCard[]
  check: CheckAnswer
  /** La carte par laquelle commencer : un lien vers une carte, comme un exercice de fin de chapitre. */
  startAt?: string | null
  /** Dit la notion de chaque carte, quand elles n'ont pas toutes la même. */
  showNotion?: boolean
  doneLabel: string
}) {
  const [position, setPosition] = useState<number | null>(null)
  const [outcomes, setOutcomes] = useState<Outcome[]>([])
  const current =
    position ??
    Math.max(
      0,
      cards.findIndex((candidate) => candidate.cardId === startAt),
    )
  const card = cards[current]
  const next = (outcome: Outcome) => {
    setOutcomes((previous) => [...previous, outcome])
    setPosition(current + 1)
  }
  const restart = () => {
    setOutcomes([])
    setPosition(0)
  }

  if (cards.length === 0) return null
  if (!card) return <Summary outcomes={outcomes} doneLabel={doneLabel} onRestart={restart} />
  return (
    <>
      <p className="text-sm text-ink-muted">
        Carte {current + 1} sur {cards.length}
        {showNotion && card.notionTitle && ` · ${card.notionTitle}`}
      </p>
      <PracticeCardView
        key={card.cardId}
        subject={subject}
        card={card}
        check={check}
        onNext={next}
      />
      <ReportButton key={`report-${card.cardId}`} target={{ cardId: card.cardId }} />
    </>
  )
}

function Summary({
  outcomes,
  doneLabel,
  onRestart,
}: {
  outcomes: Outcome[]
  doneLabel: string
  onRestart: () => void
}) {
  const right = outcomes.filter(Boolean).length
  return (
    <div className={CARD_STYLE}>
      <p role="status" className="font-medium">
        {doneLabel}
      </p>
      {outcomes.length > 0 && (
        <p className="text-ink-muted">
          {right} / {outcomes.length} juste{right > 1 ? 's' : ''}.
        </p>
      )}
      <button type="button" onClick={onRestart} className={SECONDARY}>
        Recommencer
      </button>
    </div>
  )
}

function PracticeCardView({
  subject,
  card,
  check,
  onNext,
}: {
  subject: string
  card: PracticeCard
  check: CheckAnswer
  onNext: (outcome: Outcome) => void
}) {
  if (isAnsweredOnServer(card)) {
    return <AnsweredCard subject={subject} card={card} check={check} onNext={onNext} />
  }
  if (card.type === 'CODING_EXERCISE') return <ExerciseTry card={card} onNext={onNext} />
  return <ErrorState message="Cette carte ne peut pas être corrigée : signale-la." />
}

/** Une carte à choix ou un code à trous : le serveur corrige, sans rien enregistrer. */
function AnsweredCard({
  subject,
  card,
  check: checkAnswer,
  onNext,
}: {
  subject: string
  card: PracticeCard
  check: CheckAnswer
  onNext: (outcome: Outcome) => void
}) {
  const check = useMutation({ mutationFn: checkAnswer })
  const [draft, setDraft] = useState(() => emptyDraft(card))
  const correction = check.data
  const nextCard = useFocusOnAppear<HTMLButtonElement>(correction !== undefined)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    check.mutate(toGradedAnswer(card, draft))
  }

  return (
    <form onSubmit={submit} className={CARD_STYLE}>
      <GradedQuestion
        question={card}
        label={CARD_TYPE_LABELS[card.type]}
        draft={draft}
        correction={correction}
        onChange={setDraft}
      />
      {correction && !correction.correct && card.notionSlug && (
        <LessonLink subject={subject} notion={card.notionSlug} />
      )}
      {correction ? (
        <button
          ref={nextCard}
          type="button"
          onClick={() => onNext(correction.correct)}
          className={PRIMARY}
        >
          Carte suivante
        </button>
      ) : (
        <button
          type="submit"
          disabled={!isDraftComplete(card, draft) || check.isPending}
          className={PRIMARY}
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

/**
 * Un exercice de code : on lance ses tests autant de fois qu'on veut, sans rien rendre. Il compte juste si ses tests
 * passaient à la dernière exécution.
 */
function ExerciseTry({ card, onNext }: { card: PracticeCard; onNext: (outcome: Outcome) => void }) {
  const [files, setFiles] = useState<ExerciseFile[]>(() => card.exercise?.starter ?? [])
  const run = useRunExercise()
  const edit = (path: string, content: string) =>
    setFiles((current) => current.map((file) => (file.path === path ? { path, content } : file)))
  return (
    <article className={CARD_STYLE}>
      <p className="text-sm font-medium text-ink-muted">{CARD_TYPE_LABELS[card.type]}</p>
      <Markdown>{card.prompt}</Markdown>
      <ExerciseEditor
        files={files}
        tests={card.exercise?.tests ?? []}
        readOnly={false}
        onEdit={edit}
      />
      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          disabled={run.isPending}
          onClick={() => run.mutate({ cardId: card.cardId, files })}
          className={SECONDARY}
        >
          {run.isPending ? 'Exécution…' : 'Lancer les tests'}
        </button>
        <button
          type="button"
          onClick={() => onNext(run.data?.status === 'PASSED')}
          className={PRIMARY}
        >
          Carte suivante
        </button>
      </div>
      {run.error && (
        <p role="alert" className="text-sm text-danger">
          {run.error instanceof ApiError ? run.error.message : 'L’exécution a échoué.'}
        </p>
      )}
      {run.data && <ExerciseRunResult run={run.data} />}
    </article>
  )
}
