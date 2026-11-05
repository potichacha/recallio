import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/errors'
import { useCheckTourNotion, useTourCheckpoint, type TourQuestion } from '../api/tour'
import { GradedQuestion } from '../components/GradedQuestion'
import {
  emptyDraft,
  isDraftComplete,
  toGradedAnswer,
  type AnswerDraft,
} from '../components/gradedAnswers'
import { button } from '../components/ui/button'

/**
 * Les questions qui valident une notion du Tour (ADR 0037) : on y répond, le serveur corrige, et la notion est validée
 * quand toutes sont justes. On peut réessayer autant de fois qu'on veut ; ces réponses ne comptent pas comme des
 * révisions.
 */
export function NotionCheck({ subject, notion }: { subject: string; notion: string }) {
  const checkpoint = useTourCheckpoint(subject, notion)
  const check = useCheckTourNotion(subject, notion)
  const [drafts, setDrafts] = useState<Record<string, AnswerDraft>>({})

  if (checkpoint.isError) {
    return <p role="alert">Impossible de charger les questions de cette notion.</p>
  }
  if (!checkpoint.data) return null

  const { questions } = checkpoint.data
  const draftOf = (question: TourQuestion): AnswerDraft =>
    drafts[question.cardId] ?? emptyDraft(question)
  const update = (question: TourQuestion, draft: AnswerDraft) =>
    setDrafts((current) => ({ ...current, [question.cardId]: draft }))
  const results = new Map(check.data?.results.map((result) => [result.cardId, result]))
  const validated = check.data?.validated ?? checkpoint.data.validated
  const complete = questions.every((question) => isDraftComplete(question, draftOf(question)))

  const submit = (event: FormEvent) => {
    event.preventDefault()
    check.mutate(questions.map((question) => toGradedAnswer(question, draftOf(question))))
  }

  return (
    <section
      aria-labelledby={`check-${notion}`}
      className="flex flex-col gap-4 rounded-xl bg-surface p-5 shadow-sm"
    >
      <h2 id={`check-${notion}`} className="text-lg font-semibold">
        Valider la notion
      </h2>
      {validated ? (
        <p role="status" className="font-medium text-success">
          Notion validée !
        </p>
      ) : (
        <p className="text-sm text-ink-muted">
          Réponds juste à ces questions pour valider la notion. Elles ne comptent pas dans tes
          révisions.
        </p>
      )}
      <form onSubmit={submit} className="flex flex-col gap-5">
        {questions.map((question, index) => (
          <GradedQuestion
            key={question.cardId}
            question={question}
            label={`Question ${index + 1}`}
            draft={draftOf(question)}
            correction={results.get(question.cardId)}
            onChange={(draft) => update(question, draft)}
          />
        ))}
        {check.data ? (
          !check.data.validated && (
            <button
              type="button"
              onClick={() => check.reset()}
              className={`self-start ${button({ variant: 'secondary' })}`}
            >
              Réessayer
            </button>
          )
        ) : (
          <button
            type="submit"
            disabled={!complete || check.isPending}
            className={`self-start ${button()}`}
          >
            {check.isPending ? 'Vérification…' : 'Vérifier'}
          </button>
        )}
      </form>
      {check.error && (
        <p role="alert" className="text-sm text-danger">
          {check.error instanceof ApiError ? check.error.message : 'La vérification a échoué.'}
        </p>
      )}
    </section>
  )
}
