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
      <p
        className={`font-medium ${run.status === 'PASSED' ? 'text-emerald-700' : 'text-rose-700'}`}
      >
        {STATUS_LABELS[run.status]}
      </p>
      {run.tests.length > 0 && (
        <ul className="flex flex-col gap-1 text-sm">
          {run.tests.map((test) => (
            <li key={`${test.className}.${test.name}`} className="flex flex-col">
              <span className={test.passed ? 'text-emerald-700' : 'text-rose-700'}>
                {test.passed ? '✓' : '✗'} <code className="font-mono">{test.name}</code>
              </span>
              {!test.passed && test.message && (
                <span className="pl-5 font-mono text-xs text-slate-600">{test.message}</span>
              )}
            </li>
          ))}
        </ul>
      )}
      {run.output && (
        <pre className="overflow-x-auto rounded-lg bg-slate-900 p-3 font-mono text-xs text-slate-100">
          {run.output}
        </pre>
      )}
    </section>
  )
}
