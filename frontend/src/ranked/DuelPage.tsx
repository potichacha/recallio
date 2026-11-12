import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../api/errors'
import {
  useAnswerDuel,
  useDuel,
  useNextQuestion,
  type Duel,
  type DuelAnswerResult,
  type DuelQuestion,
  type DuelTurn,
} from '../api/ranked'
import { GradedQuestion } from '../components/GradedQuestion'
import { emptyDraft, isDraftComplete, type AnswerDraft } from '../components/gradedAnswers'
import { rankLabel, TIER_LABELS } from '../components/rankLabels'
import { ShareButton } from '../components/ShareButton'
import { useCountdown } from '../components/useCountdown'
import { button } from '../components/ui/button'

const PRIMARY = `self-start ${button()}`

const OUTCOMES = { WIN: 'Victoire !', LOSS: 'Défaite', DRAW: 'Match nul' } as const

/**
 * Un duel du classé (ADR 0046) : ses questions une à une, servies par le serveur qui mesure le temps, puis l'attente de
 * l'adversaire et le résultat.
 */
export function DuelPage() {
  const { id = '' } = useParams()
  const duel = useDuel(id)

  if (duel.isPending) return <LoadingState label="Chargement du duel…" shape="card" />
  if (duel.isError) {
    return (
      <ErrorState message="Impossible de charger ce duel." onRetry={() => void duel.refetch()} />
    )
  }
  const playable = duel.data.status === 'OPEN' || duel.data.status === 'FULL'
  return (
    <section className="flex flex-col gap-5">
      <h1 className="text-2xl font-bold">
        Duel {duel.data.mode === 'LIVE' ? 'en direct' : 'classé'}
        {duel.data.opponent?.displayName ? ` contre ${duel.data.opponent.displayName}` : ''}
      </h1>
      {playable && !duel.data.me.finished ? (
        <Play duel={duel.data} />
      ) : (
        <Outcome duel={duel.data} />
      )}
      <Link to="/ranked" className="self-start text-sm text-brand hover:underline">
        Retour au classé
      </Link>
    </section>
  )
}

/** Les questions du joueur, une à une : la suivante n'est demandée qu'après la correction de la précédente. */
function Play({ duel }: { duel: Duel }) {
  const next = useNextQuestion(duel.id)
  const queryClient = useQueryClient()
  const [turn, setTurn] = useState<DuelTurn>()
  const { mutate } = next

  const ask = () =>
    mutate(undefined, {
      onSuccess: (served) => {
        if (served.finished) {
          void queryClient.invalidateQueries({ queryKey: ['ranked', 'duel', duel.id] })
        } else {
          setTurn(served)
        }
      },
    })
  // La première question part dès l'ouverture : son temps compte à partir de là.
  const asked = useRef(false)
  useEffect(() => {
    if (!asked.current) {
      asked.current = true
      ask()
    }
  })

  if (next.isError) {
    return (
      <p role="alert">
        {next.error instanceof ApiError ? next.error.message : 'La question n’a pas pu venir.'}
      </p>
    )
  }
  if (!turn?.question || turn.position == null) return <p>Question suivante…</p>
  return (
    <Question
      key={turn.position}
      duelId={duel.id}
      question={turn.question}
      position={turn.position}
      total={turn.total}
      remainingMs={turn.remainingMs ?? 0}
      onNext={ask}
      loadingNext={next.isPending}
    />
  )
}

function Question({
  duelId,
  question,
  position,
  total,
  remainingMs,
  onNext,
  loadingNext,
}: {
  duelId: string
  question: DuelQuestion
  position: number
  total: number
  remainingMs: number
  onNext: () => void
  loadingNext: boolean
}) {
  const [draft, setDraft] = useState<AnswerDraft>(() => emptyDraft(question))
  const [result, setResult] = useState<DuelAnswerResult>()
  const answer = useAnswerDuel(duelId)
  const remaining = useCountdown(remainingMs)
  const last = position === total - 1

  // Le temps écoulé sans réponse, on passe à la suivante : le serveur la compte fausse.
  const skipped = useRef(false)
  useEffect(() => {
    if (remaining === 0 && !result && !answer.isPending && !skipped.current) {
      skipped.current = true
      onNext()
    }
  })

  const submit = () =>
    answer.mutate(
      question.type === 'FILL_IN'
        ? { position, filledBlanks: draft.blanks }
        : { position, selectedChoices: draft.choices },
      { onSuccess: setResult },
    )

  return (
    <div className="flex flex-col gap-4 rounded-xl bg-surface p-5 shadow-sm">
      <div className="flex items-center justify-between gap-3">
        <span className="text-sm text-ink-muted">
          Question {position + 1} sur {total}
        </span>
        {!result && (
          <span role="timer" aria-label="Temps restant" className="font-mono text-lg">
            {Math.ceil(remaining / 1000)} s
          </span>
        )}
      </div>
      <GradedQuestion
        question={question}
        label={`Question ${position + 1}`}
        draft={draft}
        correction={result}
        onChange={setDraft}
      />
      {result ? (
        <>
          {result.late && (
            <p role="status" className="text-danger">
              Trop tard : la réponse compte fausse.
            </p>
          )}
          {last ? (
            <p className="text-sm text-ink-muted">C’était la dernière question.</p>
          ) : (
            <button type="button" onClick={onNext} disabled={loadingNext} className={PRIMARY}>
              Question suivante
            </button>
          )}
        </>
      ) : (
        <button
          type="button"
          onClick={submit}
          disabled={!isDraftComplete(question, draft) || answer.isPending}
          className={PRIMARY}
        >
          Valider
        </button>
      )}
      {answer.isError && (
        <p role="alert" className="text-sm text-danger">
          {answer.error instanceof ApiError
            ? answer.error.message
            : 'La réponse n’a pas pu partir.'}
        </p>
      )}
    </div>
  )
}

/** Le joueur a fini : l'attente de l'adversaire, puis le résultat. */
function Outcome({ duel }: { duel: Duel }) {
  const { me, opponent, result } = duel
  if (duel.status === 'EXPIRED') {
    return (
      <p role="status">
        Personne n’a rejoint ce duel à temps : il est annulé, sans effet sur le rang.
      </p>
    )
  }
  if (!result) {
    return (
      <div role="status" className="flex flex-col gap-1 rounded-xl bg-surface p-5 shadow-sm">
        <p className="font-medium">
          Tu as fini : {me.correct} / {duel.questionCount} bonnes réponses.
        </p>
        <p className="text-sm text-ink-muted">
          {opponent
            ? `${opponent.displayName ?? 'Ton adversaire'} en est à ${opponent.answered} / ${duel.questionCount}.`
            : 'Un adversaire va rejoindre ce duel : le résultat arrivera quand il aura joué.'}
        </p>
      </div>
    )
  }
  return (
    <div role="status" className="flex flex-col gap-2 rounded-xl bg-brand p-5 text-on-brand">
      <p className="text-2xl font-bold">{OUTCOMES[result.outcome]}</p>
      <p>
        {result.myCorrect} / {duel.questionCount} contre {result.opponentCorrect} /{' '}
        {duel.questionCount} ({seconds(result.myTimeMs)} contre {seconds(result.opponentTimeMs)})
      </p>
      {result.placing ? (
        <p>Match de placement : ton rang apparaîtra après le cinquième duel de la saison.</p>
      ) : (
        result.rankAfter && <p>Rang : {rankLabel(result.rankAfter)}</p>
      )}
      {result.promoted && result.rankAfter && (
        <div className="flex flex-wrap items-center gap-2">
          <span>Promotion en {TIER_LABELS[result.rankAfter.tier]} !</span>
          <ShareButton
            label="Partager ma promotion"
            card={{
              kicker: 'Classé',
              title: `Promotion en ${TIER_LABELS[result.rankAfter.tier]}`,
              detail: duel.subject,
              url: window.location.origin,
            }}
          />
        </div>
      )}
    </div>
  )
}

const seconds = (ms: number) => `${Math.round(ms / 100) / 10} s`
