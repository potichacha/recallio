import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/errors'
import { useAnswerLiveQuestion, type LiveGame, type LiveRound } from '../api/live'
import { ChoiceOption } from '../components/ChoiceOption'
import { choiceOutcome, toggleChoice } from '../components/choices'
import { Markdown } from '../components/Markdown'
import { useCountdown } from '../components/useCountdown'
import { useShuffledChoices } from '../components/useShuffledChoices'
import { button } from '../components/ui/button'

const SECOND = 1000

/**
 * Une question de la partie, de son chrono à sa correction (ADR 0022). Le composant vit le temps d'une question (une
 * `key` par question) : la sélection survit au passage à la correction, pour montrer ce qu'on avait coché.
 */
export function LiveRoundView({
  game,
  round,
  me,
}: {
  game: LiveGame
  round: LiveRound
  me: string | undefined
}) {
  const [selected, setSelected] = useState<number[]>([])
  const choices = useShuffledChoices(`${game.id}:${round.index}`, round.choices)
  const answer = useAnswerLiveQuestion(game.id)
  const reveal = round.reveal
  // Après une reconnexion, la sélection est perdue, mais le serveur sait que l'on a répondu.
  const answered =
    answer.isSuccess || game.players.some((player) => player.userId === me && player.answered)
  const locked = answered || answer.isPending || reveal != null
  const myPoints = game.standings.find((standing) => standing.userId === me)?.lastPoints

  const submit = (event: FormEvent) => {
    event.preventDefault()
    answer.mutate({ question: round.index, selectedChoices: [...selected].sort((a, b) => a - b) })
  }

  return (
    <article className="flex flex-col gap-4 rounded-xl border border-line bg-surface p-5 shadow-sm">
      <header className="flex items-center justify-between gap-2 text-sm">
        <span className="font-medium text-brand">
          Question {round.index + 1} / {round.total}
        </span>
        {/* Un décompte par étape : chrono de la question, puis attente de la suivante. */}
        <Countdown
          key={`${round.index}-${game.status}`}
          remainingMs={round.remainingMs}
          label={reveal ? 'Avant la suite' : 'Temps restant'}
        />
      </header>
      <Markdown>{round.prompt}</Markdown>
      <form onSubmit={submit} className="flex flex-col gap-3">
        <fieldset className="flex flex-col gap-2">
          <legend className="mb-1 text-sm text-ink-muted">
            {round.multipleAnswers ? 'Plusieurs réponses possibles' : 'Une seule réponse'}
          </legend>
          {choices.map(({ index, text }) => (
            <ChoiceOption
              key={index}
              text={text}
              type={round.multipleAnswers ? 'checkbox' : 'radio'}
              checked={selected.includes(index)}
              onChange={() =>
                setSelected((current) => toggleChoice(current, index, round.multipleAnswers))
              }
              disabled={locked}
              outcome={
                reveal
                  ? choiceOutcome(reveal.correctChoices, index, selected.includes(index))
                  : undefined
              }
            />
          ))}
        </fieldset>
        {!reveal && !answered && (
          <button
            type="submit"
            disabled={selected.length === 0 || answer.isPending}
            className={`self-start ${button()}`}
          >
            Valider
          </button>
        )}
      </form>
      {answer.isError && (
        <p role="alert">
          {answer.error instanceof ApiError
            ? answer.error.message
            : 'La réponse n’a pas pu être envoyée.'}
        </p>
      )}
      {!reveal && answered && (
        <p role="status" className="text-ink-muted">
          Réponse envoyée. {round.answeredCount} / {game.players.length} joueurs ont répondu.
        </p>
      )}
      {reveal && (
        <div className="flex flex-col gap-2 border-t border-line pt-4">
          {myPoints !== undefined && (
            <p className="text-lg font-semibold text-brand-strong">
              {myPoints > 0 ? `+${myPoints} points` : 'Aucun point sur cette question'}
            </p>
          )}
          <p role="status" className="font-medium">
            {reveal.correctCount} joueur{reveal.correctCount > 1 ? 's' : ''} sur{' '}
            {game.players.length} {reveal.correctCount > 1 ? 'ont' : 'a'} trouvé.
          </p>
          <Markdown>{reveal.explanation}</Markdown>
        </div>
      )}
    </article>
  )
}

function Countdown({ remainingMs, label }: { remainingMs: number; label: string }) {
  const remaining = useCountdown(remainingMs)
  return (
    <span role="timer" aria-label={label} className="font-mono text-ink-soft">
      {Math.ceil(remaining / SECOND)} s
    </span>
  )
}
