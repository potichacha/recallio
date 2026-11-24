import { useState, type FormEvent, type ReactNode } from 'react'
import { ApiError } from '../api/errors'
import { useRecordReview, type QueuedCard, type ReviewResult } from '../api/review'
import { BlankVerdict } from '../components/BlankVerdict'
import { LessonLink } from '../components/LessonLink'
import { Markdown } from '../components/Markdown'
import { useFocusOnAppear } from '../components/useFocusOnAppear'
import { numberBlanks } from './blanks'
import { CARD_ACTION, CARD_FRAME } from './cardFrame'
import { CardHeader } from './CardHeader'
import { button } from '../components/ui/button'
import { VerdictPanel } from './VerdictPanel'

/**
 * Un code à trous : on remplit chaque trou, on valide, et le serveur corrige trou par trou (ADR 0027). Comme pour un
 * QCM, la note n'est pas choisie ; la correction reste affichée, dans le panneau du verdict, jusqu'à ce qu'on passe à
 * la suite.
 *
 * @param report « Signaler une erreur », sous « Valider » puis dans le panneau du verdict
 */
export function FillInCard({
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
  const [filled, setFilled] = useState<string[]>(() => Array(card.blankCount).fill(''))
  const record = useRecordReview()
  const result = record.data
  const nextCard = useFocusOnAppear<HTMLButtonElement>(result?.correction != null)
  const blanks = result?.correction?.blanks
  const correct = result?.rating === 'GOOD'

  const fill = (index: number, value: string) =>
    setFilled((current) => current.map((previous, other) => (other === index ? value : previous)))

  const submit = (event?: FormEvent) => {
    event?.preventDefault()
    record.mutate({
      requestId,
      cardId: card.cardId,
      filledBlanks: filled,
      elapsedMs: Date.now() - shownAt,
    })
  }

  return (
    <article className={CARD_FRAME}>
      <CardHeader card={card} />
      <Markdown>{numberBlanks(card.prompt)}</Markdown>
      <form onSubmit={submit} className="flex flex-1 flex-col gap-3">
        {filled.map((value, index) => (
          <label key={index} className="flex flex-col gap-1">
            <span className="text-sm font-extrabold">Trou {index + 1}</span>
            <input
              value={value}
              onChange={(event) => fill(index, event.target.value)}
              readOnly={result !== undefined}
              spellCheck={false}
              autoCapitalize="off"
              autoComplete="off"
              className="min-h-11 rounded-xl border-2 border-line-strong bg-surface px-3 py-2 font-mono text-sm"
            />
            {blanks?.[index] && (
              <BlankVerdict correct={blanks[index].correct} expected={blanks[index].expected} />
            )}
          </label>
        ))}
        {!result && (
          <button
            type="submit"
            disabled={filled.some((value) => value.trim() === '') || record.isPending}
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
          title={correct ? 'Tous les trous sont justes !' : 'Pas tout à fait'}
          detail={correct ? undefined : 'La réponse attendue est indiquée sous chaque trou faux.'}
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
