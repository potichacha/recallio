import { useState, type FormEvent } from 'react'
import { useCheckPracticeAnswer, usePractice, type PracticeCard } from '../api/practice'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'
import { FlipCard } from '../components/FlipCard'
import { GradedQuestion } from '../components/GradedQuestion'
import { emptyDraft, isDraftComplete, toGradedAnswer } from '../components/gradedAnswers'
import { Markdown } from '../components/Markdown'
import { button } from '../components/ui/button'

/** Le nombre de questions à retourner sous le cours d'une page : de quoi se tester, pas une séance. */
const QUESTIONS = 2

const FACE = 'flex flex-col gap-3 rounded-xl border border-line bg-surface p-4 shadow-sm'

/**
 * « Teste-toi », dans le cours d'une page du Tour (ADR 0050) : quelques questions de cours de la notion. On choisit sa
 * réponse et le serveur la corrige (ADR 0051) ; une question qui n'a pas encore de choix se retourne pour voir la
 * réponse. Rien n'est enregistré : la révision espacée les reprendra, une par jour.
 */
export function TestYourself({ subject, notion }: { subject: string; notion: string }) {
  const practice = usePractice(subject, notion)
  const questions = (practice.data?.cards ?? [])
    .filter((card) => card.type === 'FLASHCARD' || card.type === 'DEFINITION')
    .filter((card) => card.choices.length > 0 || card.answer)
    .slice(0, QUESTIONS)
  if (questions.length === 0) return null

  return (
    <section aria-labelledby={`test-${notion}`} className="flex flex-col gap-3">
      <h2 id={`test-${notion}`} className="text-lg font-semibold">
        Teste-toi
      </h2>
      {questions.map((card) =>
        card.choices.length > 0 ? (
          <ChoiceQuestion key={card.cardId} subject={subject} notion={notion} card={card} />
        ) : (
          <Question key={card.cardId} card={card} />
        ),
      )}
    </section>
  )
}

/** Une question à choix : on choisit, et le serveur corrige. */
function ChoiceQuestion({
  subject,
  notion,
  card,
}: {
  subject: string
  notion: string
  card: PracticeCard
}) {
  const check = useCheckPracticeAnswer(subject, notion)
  const [draft, setDraft] = useState(() => emptyDraft(card))

  const submit = (event: FormEvent) => {
    event.preventDefault()
    check.mutate(toGradedAnswer(card, draft))
  }

  return (
    <form onSubmit={submit} className={FACE}>
      <GradedQuestion
        question={card}
        label={CARD_TYPE_LABELS[card.type]}
        draft={draft}
        correction={check.data}
        onChange={setDraft}
      />
      {!check.data && (
        <button
          type="submit"
          disabled={!isDraftComplete(card, draft) || check.isPending}
          className={`self-start ${button({ variant: 'secondary', size: 'sm' })}`}
        >
          {check.isPending ? 'Vérification…' : 'Vérifier'}
        </button>
      )}
      {check.isError && (
        <p role="alert" className="text-sm text-danger">
          La vérification a échoué.
        </p>
      )}
    </form>
  )
}

/** Une question qui n'a pas encore de choix : on la retourne pour voir la réponse. */
function Question({ card }: { card: PracticeCard }) {
  const [flipped, setFlipped] = useState(false)
  return (
    <FlipCard
      flipped={flipped}
      front={
        <div className={FACE}>
          <Markdown>{card.prompt}</Markdown>
          <button
            type="button"
            onClick={() => setFlipped(true)}
            className={`self-start ${button({ variant: 'secondary', size: 'sm' })}`}
          >
            Voir la réponse
          </button>
        </div>
      }
      back={
        <div className={FACE}>
          <section aria-label="Réponse" className="flex flex-col gap-2">
            <Markdown>{card.answer ?? ''}</Markdown>
          </section>
          <button
            type="button"
            onClick={() => setFlipped(false)}
            className={`self-start ${button({ variant: 'ghost', size: 'sm' })}`}
          >
            Retourner la carte
          </button>
        </div>
      }
    />
  )
}
