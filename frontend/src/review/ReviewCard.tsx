import { useState } from 'react'
import type { QueuedCard, ReviewResult } from '../api/review'
import { ErrorState } from '../components/states/ErrorState'
import { ExerciseCard } from './ExerciseCard'
import { FillInCard } from './FillInCard'
import { LessonCard } from './LessonCard'
import { MultipleChoiceCard } from './MultipleChoiceCard'

/**
 * Une carte à réviser, que le serveur corrige toujours (ADR 0063) : une carte à choix (tout format qui en a, ADR 0051),
 * un code à trous, ou un exercice de code jugé par ses tests. La leçon de la notion, s'il faut la lire, passe avant.
 * La file ne sert pas d'autre carte ; si l'une arrivait quand même, on le dit plutôt que de la proposer sans correction.
 */
export function ReviewCard({
  card,
  onReviewed,
}: {
  card: QueuedCard
  onReviewed: (result: ReviewResult) => void
}) {
  const [lessonRead, setLessonRead] = useState(false)
  if (card.lesson && !lessonRead) {
    return <LessonCard card={card} onRead={() => setLessonRead(true)} />
  }
  if (card.choices.length > 0) {
    return <MultipleChoiceCard card={card} onReviewed={onReviewed} />
  }
  if (card.type === 'FILL_IN') {
    return <FillInCard card={card} onReviewed={onReviewed} />
  }
  if (card.type === 'CODING_EXERCISE') {
    return <ExerciseCard card={card} onReviewed={onReviewed} />
  }
  return (
    <ErrorState message="Cette carte ne peut pas être corrigée : signale-la, puis termine la session." />
  )
}
