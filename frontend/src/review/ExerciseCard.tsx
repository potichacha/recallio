import { useState } from 'react'
import { ApiError } from '../api/errors'
import { useRecordReview, useRunExercise, type ExerciseFile, type QueuedCard } from '../api/review'
import { Markdown } from '../components/Markdown'
import { SourceFiles } from '../components/SourceFiles'
import { CardHeader } from './CardHeader'
import { ExerciseRunResult } from './ExerciseRunResult'

/**
 * Un exercice de code (ADR 0029, 0030) : on modifie le code de départ, on lance les tests autant de fois qu'on veut,
 * puis on rend son code. Les tests le jugent, et la solution apparaît avec la correction.
 */
export function ExerciseCard({ card, onReviewed }: { card: QueuedCard; onReviewed: () => void }) {
  const [shownAt] = useState(() => Date.now())
  const [requestId] = useState(() => crypto.randomUUID())
  const [files, setFiles] = useState<ExerciseFile[]>(() => card.exercise?.starter ?? [])
  const run = useRunExercise()
  const record = useRecordReview()
  const correction = record.data?.correction
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
    <article className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <CardHeader card={card} />
      <Markdown>{card.prompt}</Markdown>

      {files.map((file) => (
        <label key={file.path} className="flex flex-col gap-1">
          <span className="font-mono text-xs text-slate-500">{file.path}</span>
          <textarea
            value={file.content}
            onChange={(event) => edit(file.path, event.target.value)}
            readOnly={correction !== undefined}
            rows={Math.min(20, file.content.split('\n').length + 2)}
            spellCheck={false}
            autoCapitalize="off"
            autoComplete="off"
            className="rounded-lg border border-slate-300 px-3 py-2 font-mono text-sm"
          />
        </label>
      ))}
      <SourceFiles title="Tests" files={card.exercise?.tests ?? []} />

      {!correction && (
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            disabled={busy}
            onClick={() => run.mutate({ cardId: card.cardId, files })}
            className="rounded-lg border border-indigo-200 px-4 py-2 font-medium text-indigo-700 hover:bg-indigo-50 disabled:opacity-50"
          >
            {run.isPending ? 'Exécution…' : 'Lancer les tests'}
          </button>
          <button
            type="button"
            disabled={busy}
            onClick={submit}
            className="rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            {record.isPending ? 'Correction…' : 'Rendre'}
          </button>
        </div>
      )}

      {error && (
        <p role="alert" className="text-sm text-rose-700">
          {error instanceof ApiError ? error.message : 'L’exécution a échoué.'}
        </p>
      )}

      {!correction && run.data && <ExerciseRunResult run={run.data} />}

      {correction && (
        <div className="flex flex-col gap-3 border-t border-slate-100 pt-4">
          <p role="status" className="font-medium">
            {record.data?.rating === 'GOOD'
              ? 'Exercice réussi !'
              : 'Pas encore : compare avec la solution.'}
          </p>
          {correction.exerciseRun && <ExerciseRunResult run={correction.exerciseRun} />}
          <SourceFiles title="Solution" files={correction.solution} />
          <Markdown>{correction.explanation}</Markdown>
          <button
            type="button"
            onClick={onReviewed}
            className="self-start rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700"
          >
            Carte suivante
          </button>
        </div>
      )}
    </article>
  )
}
