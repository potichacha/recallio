import { useState } from 'react'
import { ApiError } from '../api/errors'
import {
  useRecordReview,
  useRunExercise,
  type ExerciseFile,
  type QueuedCard,
  type ReviewResult,
} from '../api/review'
import { Markdown } from '../components/Markdown'
import { SourceFiles } from '../components/SourceFiles'
import { CardHeader } from './CardHeader'
import { ExerciseEditor } from './ExerciseEditor'
import { ExerciseRunResult } from './ExerciseRunResult'
import { button } from '../components/ui/button'

/**
 * Un exercice de code (ADR 0029, 0030) : on modifie le code de départ dans l'éditeur, un onglet par fichier, on lance
 * les tests autant de fois qu'on veut, puis on rend son code. Les tests le jugent, et la solution apparaît avec la correction.
 */
export function ExerciseCard({
  card,
  onReviewed,
}: {
  card: QueuedCard
  onReviewed: (result: ReviewResult) => void
}) {
  const [shownAt] = useState(() => Date.now())
  const [requestId] = useState(() => crypto.randomUUID())
  const [files, setFiles] = useState<ExerciseFile[]>(() => card.exercise?.starter ?? [])
  const run = useRunExercise()
  const record = useRecordReview()
  const result = record.data
  const correction = result?.correction
  const busy = run.isPending || record.isPending

  const edit = (path: string, content: string) =>
    setFiles((current) => current.map((file) => (file.path === path ? { path, content } : file)))

  const submit = () =>
    record.mutate({
      requestId,
      cardId: card.cardId,
      submittedFiles: files,
      elapsedMs: Date.now() - shownAt,
    })

  const error = run.error ?? record.error

  return (
    <article className="flex flex-col gap-4 rounded-xl border border-line bg-surface p-5 shadow-sm">
      <CardHeader card={card} />
      <Markdown>{card.prompt}</Markdown>

      <ExerciseEditor
        files={files}
        tests={card.exercise?.tests ?? []}
        readOnly={correction !== undefined}
        onEdit={edit}
      />

      {!correction && (
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            disabled={busy}
            onClick={() => run.mutate({ cardId: card.cardId, files })}
            className={button({ variant: 'secondary' })}
          >
            {run.isPending ? 'Exécution…' : 'Lancer les tests'}
          </button>
          <button type="button" disabled={busy} onClick={submit} className={button()}>
            {record.isPending ? 'Correction…' : 'Rendre'}
          </button>
        </div>
      )}

      {error && (
        <p role="alert" className="text-sm text-danger">
          {error instanceof ApiError ? error.message : 'L’exécution a échoué.'}
        </p>
      )}

      {!correction && run.data && <ExerciseRunResult run={run.data} />}

      {correction && (
        <div className="flex flex-col gap-3 border-t border-line pt-4">
          <p role="status" className="font-medium">
            {result?.rating === 'GOOD'
              ? 'Exercice réussi !'
              : 'Pas encore : compare avec la solution.'}
          </p>
          {correction.exerciseRun && <ExerciseRunResult run={correction.exerciseRun} />}
          <SourceFiles title="Solution" files={correction.solution} />
          <Markdown>{correction.explanation}</Markdown>
          <button
            type="button"
            onClick={() => result && onReviewed(result)}
            className={`self-start ${button()}`}
          >
            Carte suivante
          </button>
        </div>
      )}
    </article>
  )
}
