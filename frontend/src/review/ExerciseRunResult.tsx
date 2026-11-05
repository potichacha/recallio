import { Check, X } from '@phosphor-icons/react'
import type { ExerciseRun } from '../api/review'

const STATUS_LABELS: Record<ExerciseRun['status'], string> = {
  PASSED: 'Tous les tests passent !',
  FAILED: 'Des tests échouent.',
  COMPILATION_ERROR: 'Le code ne compile pas.',
  TIMEOUT: 'Le code ne s’est pas arrêté à temps (une boucle infinie ?).',
}

/** Le résultat d'une exécution : chaque test, réussi ou non, ou les erreurs de compilation. */
export function ExerciseRunResult({ run }: { run: ExerciseRun }) {
  return (
    <section aria-label="Résultat des tests" className="flex flex-col gap-2">
      <p className={`font-medium ${run.status === 'PASSED' ? 'text-success' : 'text-danger'}`}>
        {STATUS_LABELS[run.status]}
      </p>
      {run.tests.length > 0 && (
        <ul className="flex flex-col gap-1 text-sm">
          {run.tests.map((test) => (
            <li key={`${test.className}.${test.name}`} className="flex flex-col">
              <span
                className={`inline-flex items-center gap-1 ${test.passed ? 'text-success' : 'text-danger'}`}
              >
                {test.passed ? (
                  <Check weight="bold" aria-hidden />
                ) : (
                  <X weight="bold" aria-hidden />
                )}
                <span className="sr-only">{test.passed ? 'Réussi :' : 'Échoué :'}</span>
                <code className="font-mono">{test.name}</code>
              </span>
              {!test.passed && test.message && (
                <span className="pl-5 font-mono text-xs text-ink-muted">{test.message}</span>
              )}
            </li>
          ))}
        </ul>
      )}
      {run.output && (
        <pre className="overflow-x-auto rounded-lg bg-code p-3 font-mono text-xs text-code-ink">
          {run.output}
        </pre>
      )}
    </section>
  )
}
