import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import {
  useAnswerChallengeCard,
  useChallenge,
  useNextChallengeCard,
  type Challenge,
  type ChallengeCard,
} from '../api/challenges'
import { ApiError } from '../api/errors'
import { ChoiceOption } from '../components/ChoiceOption'
import { choiceOutcome, toggleChoice } from '../components/choices'
import { Markdown } from '../components/Markdown'
import { useCountdown } from '../components/useCountdown'

const SECOND = 1000

/** Un défi de groupe (ADR 0020) : ses règles, la partie carte par carte, puis le classement. */
export function ChallengePage() {
  const { challengeId = '' } = useParams()
  const challenge = useChallenge(challengeId)
  const [playing, setPlaying] = useState(false)
  const stopPlaying = useCallback(() => setPlaying(false), [])

  if (challenge.error instanceof ApiError && challenge.error.status === 404) {
    return (
      <section className="flex flex-col items-start gap-3">
        <h1 className="text-2xl font-bold">Défi introuvable</h1>
        <Link to="/groups" className="text-indigo-600 hover:underline">
          Mes groupes
        </Link>
      </section>
    )
  }
  if (challenge.isError) return <p role="alert">Impossible de charger le défi.</p>
  if (challenge.isPending) return <p>Chargement du défi…</p>

  const { summary, groupId, groupName } = challenge.data
  const canPlay = summary.open && summary.myStatus !== 'FINISHED'
  return (
    <section className="flex flex-col gap-4">
      <Link to={`/groups/${groupId}`} className="text-sm text-indigo-600 hover:underline">
        ← {groupName}
      </Link>
      <h1 className="text-2xl font-bold">{summary.title}</h1>
      <p className="text-slate-600">
        {summary.cardCount} QCM, {summary.secondsPerCard} secondes par carte. Une réponse hors délai
        compte comme fausse ; le chrono continue si vous quittez la page.
      </p>
      {playing ? (
        <ChallengePlay challengeId={challengeId} onFinished={stopPlaying} />
      ) : (
        <>
          {canPlay && (
            <button
              type="button"
              onClick={() => setPlaying(true)}
              className="self-start rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700"
            >
              {summary.myStatus === 'IN_PROGRESS' ? 'Reprendre' : 'Commencer'}
            </button>
          )}
          {!summary.open && summary.myStatus !== 'FINISHED' && (
            <p>Ce défi est terminé : on ne peut plus y jouer.</p>
          )}
          <Results challenge={challenge.data} />
        </>
      )}
    </section>
  )
}

/** La partie : le serveur donne la carte à jouer, et dit quand la partie est terminée. */
function ChallengePlay({
  challengeId,
  onFinished,
}: {
  challengeId: string
  onFinished: () => void
}) {
  const next = useNextChallengeCard(challengeId)
  const { mutate } = next
  const step = next.data

  useEffect(() => mutate(), [mutate])
  useEffect(() => {
    if (step?.finished) onFinished()
  }, [step, onFinished])

  if (next.isError) {
    return (
      <div role="alert" className="flex flex-col items-start gap-2">
        <p>
          {next.error instanceof ApiError
            ? next.error.message
            : 'La carte n’a pas pu être chargée.'}
        </p>
        <button type="button" onClick={() => mutate()} className="font-medium underline">
          Réessayer
        </button>
      </div>
    )
  }
  if (!step?.card || next.isPending) return <p>Chargement de la carte…</p>
  return (
    <ChallengeCardPlay
      key={step.card.position}
      challengeId={challengeId}
      card={step.card}
      cardCount={step.cardCount}
      onNext={() => mutate()}
    />
  )
}

function ChallengeCardPlay({
  challengeId,
  card,
  cardCount,
  onNext,
}: {
  challengeId: string
  card: ChallengeCard
  cardCount: number
  onNext: () => void
}) {
  const remaining = useCountdown(card.remainingMs)
  const [selected, setSelected] = useState<number[]>([])
  const answer = useAnswerChallengeCard(challengeId)
  const result = answer.data
  const timeUp = remaining === 0 && !result && !answer.isPending

  const submit = (event: FormEvent) => {
    event.preventDefault()
    answer.mutate({ position: card.position, selectedChoices: [...selected].sort((a, b) => a - b) })
  }

  return (
    <article className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <header className="flex items-center justify-between gap-2 text-sm">
        <span className="font-medium text-indigo-600">
          Carte {card.position + 1} / {cardCount}
        </span>
        {!result && (
          <span role="timer" aria-label="Temps restant" className="font-mono text-slate-700">
            {Math.ceil(remaining / SECOND)} s
          </span>
        )}
      </header>
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
              onChange={() =>
                setSelected((current) => toggleChoice(current, index, card.multipleAnswers))
              }
              disabled={timeUp}
              outcome={
                result
                  ? choiceOutcome(result.correctChoices, index, selected.includes(index))
                  : undefined
              }
            />
          ))}
        </fieldset>
        {!result && !timeUp && (
          <button
            type="submit"
            disabled={selected.length === 0 || answer.isPending}
            className="self-start rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            Valider
          </button>
        )}
      </form>
      {answer.isError && <p role="alert">La réponse n’a pas pu être envoyée.</p>}
      {timeUp && (
        <NextStep message="Temps écoulé : cette carte compte comme fausse." onNext={onNext} />
      )}
      {result && (
        <NextStep
          message={
            result.correct
              ? 'Bonne réponse !'
              : result.timedOut
                ? 'Trop tard : la réponse compte comme fausse.'
                : 'Pas tout à fait : les bonnes réponses sont marquées.'
          }
          explanation={result.explanation}
          last={result.finished}
          onNext={onNext}
        />
      )}
    </article>
  )
}

function NextStep({
  message,
  explanation,
  last = false,
  onNext,
}: {
  message: string
  explanation?: string
  last?: boolean
  onNext: () => void
}) {
  return (
    <div className="flex flex-col gap-3 border-t border-slate-100 pt-4">
      <p role="status" className="font-medium">
        {message}
      </p>
      {explanation && <Markdown>{explanation}</Markdown>}
      <button
        type="button"
        onClick={onNext}
        className="self-start rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700"
      >
        {last ? 'Voir les résultats' : 'Carte suivante'}
      </button>
    </div>
  )
}

/** Le classement des parties terminées, selon la visibilité choisie pour le groupe. */
function Results({ challenge }: { challenge: Challenge }) {
  const { summary, standings, visibility } = challenge
  return (
    <section aria-labelledby="results-title" className="flex flex-col gap-2">
      <h2 id="results-title" className="text-lg font-semibold">
        Résultats
      </h2>
      {summary.myScore != null && (
        <p className="font-medium">
          Votre score : {summary.myScore} / {summary.cardCount}
        </p>
      )}
      {visibility === 'HIDDEN' ? (
        <p className="text-sm text-slate-600">Le classement de ce groupe est désactivé.</p>
      ) : standings.length === 0 ? (
        <p className="text-sm text-slate-600">Personne n’a encore terminé ce défi.</p>
      ) : (
        <ol className="flex flex-col gap-2">
          {standings.map((standing, index) => (
            <li
              key={index}
              className={`flex items-center justify-between gap-3 rounded-lg px-3 py-2 ${
                standing.me ? 'bg-indigo-50 font-medium' : 'bg-slate-50'
              }`}
            >
              <span className="flex items-center gap-3">
                <span className="w-6 text-right text-slate-500">{standing.rank}</span>
                <span>
                  {standing.displayName ?? 'Membre anonyme'}
                  {standing.me && <span className="text-slate-500"> (vous)</span>}
                </span>
              </span>
              <span className="text-sm text-slate-600">
                {standing.score} / {summary.cardCount} · {Math.round(standing.timeMs / SECOND)} s
              </span>
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}
