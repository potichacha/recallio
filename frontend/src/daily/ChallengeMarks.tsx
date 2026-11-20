import { CheckCircle, Circle, XCircle } from '@phosphor-icons/react'
import type { DailyChallenge } from '../api/dailyChallenges'

/**
 * La suite des réussites et des erreurs d'un défi du jour (ADR 0067) : une coche pleine, une croix, ou un rond vide pour
 * une question à venir. La forme et le texte lu par un lecteur d'écran disent le résultat, pas seulement la couleur.
 */
export function ChallengeMarks({ challenge }: { challenge: DailyChallenge }) {
  const answers = new Map(challenge.answers.map((answer) => [answer.position, answer.correct]))
  return (
    <ol aria-label="Tes réponses" className="flex flex-wrap gap-2">
      {challenge.questions.map((question, index) => {
        const correct = answers.get(question.position)
        const label = correct === undefined ? 'à venir' : correct ? 'juste' : 'fausse'
        return (
          <li key={question.position} className="flex">
            {correct === undefined ? (
              <Circle weight="bold" aria-hidden className="size-8 text-ink-faint" />
            ) : correct ? (
              <CheckCircle weight="fill" aria-hidden className="size-8 text-success" />
            ) : (
              <XCircle weight="bold" aria-hidden className="size-8 text-danger" />
            )}
            <span className="sr-only">
              Question {index + 1} : {label}
            </span>
          </li>
        )
      })}
    </ol>
  )
}
