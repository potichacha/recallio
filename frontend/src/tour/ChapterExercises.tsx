import { Link } from 'react-router'
import type { TourExercise } from '../api/tour'
import { practicePath } from '../practice/practicePath'

/**
 * Les exercices de code d'un chapitre du Tour (ADR 0029, 0037), à faire une fois ses pages lues. Chacun s'ouvre dans
 * l'entraînement de sa notion (ADR 0038), où ses tests le jugent.
 */
export function ChapterExercises({
  subject,
  title,
  exercises,
}: {
  subject: string
  title: string
  exercises: TourExercise[]
}) {
  return (
    <section aria-label={`Exercices du chapitre ${title}`} className="flex flex-col gap-1">
      <p className="text-sm font-medium text-slate-600">Exercices du chapitre</p>
      <ul className="flex flex-col gap-1">
        {exercises.map((exercise) => (
          <li key={exercise.cardId}>
            <Link
              to={practicePath(subject, exercise.notion, { from: 'tour', card: exercise.cardId })}
              className="text-sm text-indigo-700 hover:underline"
            >
              {exercise.summary.replaceAll('`', '')}
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
