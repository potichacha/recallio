import { useState } from 'react'
import { usePractice, type PracticeCard } from '../api/practice'
import { FlipCard } from '../components/FlipCard'
import { Markdown } from '../components/Markdown'
import { button } from '../components/ui/button'

/** Le nombre de questions à retourner sous le cours d'une page : de quoi se tester, pas une séance. */
const QUESTIONS = 2

const FACE = 'flex flex-col gap-3 rounded-xl border border-line bg-surface p-4 shadow-sm'

/**
 * « Teste-toi », dans le cours d'une page du Tour (ADR 0050) : quelques questions de la notion, qu'on retourne pour voir
 * la réponse. Rien n'est enregistré : la révision espacée les reprendra, une par jour.
 */
export function TestYourself({ subject, notion }: { subject: string; notion: string }) {
  const practice = usePractice(subject, notion)
  const questions = (practice.data?.cards ?? [])
    .filter((card) => (card.type === 'FLASHCARD' || card.type === 'DEFINITION') && card.answer)
    .slice(0, QUESTIONS)
  if (questions.length === 0) return null

  return (
    <section aria-labelledby={`test-${notion}`} className="flex flex-col gap-3">
      <h2 id={`test-${notion}`} className="text-lg font-semibold">
        Teste-toi
      </h2>
      {questions.map((card) => (
        <Question key={card.cardId} card={card} />
      ))}
    </section>
  )
}

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
