import { useState, type FormEvent, type ReactNode } from 'react'
import { ApiError } from '../api/errors'
import { useRecordReview, type QueuedCard, type ReviewResult } from '../api/review'
import { ChoiceOption } from '../components/ChoiceOption'
import { choiceOutcome, toggleChoice } from '../components/choices'
import { LessonLink } from '../components/LessonLink'
import { Markdown } from '../components/Markdown'
import { useFocusOnAppear } from '../components/useFocusOnAppear'
import { useShuffledChoices } from '../components/useShuffledChoices'
import { CARD_ACTION, CARD_FRAME } from './cardFrame'
import { CardHeader } from './CardHeader'
import { button } from '../components/ui/button'
import { VerdictPanel } from './VerdictPanel'

/**
 * Une carte à choix (un QCM, un vrai ou faux, ou une carte d'un autre format qui a reçu des choix, ADR 0051) : on coche,
 * on valide, et le serveur corrige (ADR 0019). La note n'est pas choisie : juste, la carte est « retrouvée » ; fausse,
 * elle est à revoir. Les choix sont mélangés, et la correction reste affichée, dans le panneau du verdict, jusqu'à ce
 * qu'on passe à la suite.
 *
 * @param report « Signaler une erreur », sous « Valider » puis dans le panneau du verdict
 */
export function MultipleChoiceCard({
  card,
  onReviewed,
  report,
}: {
  card: QueuedCard
  onReviewed: (result: ReviewResult) => void
  report?: ReactNode
}) {
  const [shownAt] = useState(() => Date.now())
  const [requestId] = useState(() => crypto.randomUUID())
  const [selected, setSelected] = useState<number[]>([])
  const choices = useShuffledChoices(card.cardId, card.choices)
  const record = useRecordReview()
  const result = record.data
  const nextCard = useFocusOnAppear<HTMLButtonElement>(result?.correction != null)
  const correct = result?.rating === 'GOOD'

  const toggle = (index: number) =>
    setSelected((current) => toggleChoice(current, index, card.multipleAnswers))

  const submit = (event?: FormEvent) => {
    event?.preventDefault()
    record.mutate({
      requestId,
      cardId: card.cardId,
      selectedChoices: [...selected].sort((a, b) => a - b),
      elapsedMs: Date.now() - shownAt,
    })
  }

  return (
    <article className={CARD_FRAME}>
      <CardHeader card={card} />
      <Markdown tone="question">{card.prompt}</Markdown>
      <form onSubmit={submit} className="flex flex-1 flex-col gap-4">
        <fieldset className="flex flex-col gap-2.5">
          <legend className="mb-2 text-sm font-bold text-ink-soft">
            {card.multipleAnswers ? 'Plusieurs réponses possibles' : 'Une seule réponse'}
          </legend>
          {choices.map(({ index, text }) => (
            <ChoiceOption
              key={index}
              text={text}
              type={card.multipleAnswers ? 'checkbox' : 'radio'}
              checked={selected.includes(index)}
              onChange={() => toggle(index)}
              outcome={
                result?.correction
                  ? choiceOutcome(result.correction.correctChoices, index, selected.includes(index))
                  : undefined
              }
            />
          ))}
        </fieldset>
        {!result && (
          <button
            type="submit"
            disabled={selected.length === 0 || record.isPending}
            className={`mt-auto ${CARD_ACTION} ${button({ size: 'lg' })}`}
          >
            Valider
          </button>
        )}
      </form>

      {record.isError && (
        <div role="alert" className="flex items-center gap-3 text-sm font-bold text-danger">
          <span>
            {record.error instanceof ApiError
              ? record.error.message
              : "La réponse n'a pas pu être enregistrée."}
          </span>
          <button type="button" onClick={() => submit()} className="font-extrabold underline">
            Réessayer
          </button>
        </div>
      )}

      {!result && report && <div className="flex justify-center pb-3 md:pb-0">{report}</div>}

      {result?.correction && (
        <VerdictPanel
          correct={correct}
          title={correct ? 'Bien joué !' : 'Pas tout à fait'}
          detail={correct ? undefined : 'Les bonnes réponses sont marquées.'}
          nextRef={nextCard}
          onNext={() => onReviewed(result)}
          links={
            <>
              {report}
              {!correct && card.notionSlug && (
                <LessonLink subject={card.subjectSlug} notion={card.notionSlug} />
              )}
            </>
          }
        >
          <Markdown>{result.correction.explanation}</Markdown>
        </VerdictPanel>
      )}
    </article>
  )
}
