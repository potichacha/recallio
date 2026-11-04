import type { ProgramRun } from '../api/tour'

/** Ce qu'a écrit un programme du Tour, et comment il s'est terminé (ADR 0037). */
export function ProgramOutput({ run }: { run: ProgramRun }) {
  return (
    <section aria-label="Sortie du programme" className="flex flex-col gap-2">
      <Outcome run={run} />
      {run.status === 'COMPILATION_ERROR' ? (
        <pre className="overflow-x-auto rounded-lg bg-slate-900 p-3 font-mono text-xs text-rose-200">
          {run.message}
        </pre>
      ) : (
        <>
          {run.stdout ? (
            <pre
              aria-label="Sortie standard"
              className="overflow-x-auto rounded-lg bg-slate-900 p-3 font-mono text-sm text-slate-100"
            >
              {run.stdout}
            </pre>
          ) : (
            <p className="text-sm text-slate-500">Le programme n’a rien affiché.</p>
          )}
          {run.stderr && (
            <pre
              aria-label="Sortie d'erreur"
              className="overflow-x-auto rounded-lg bg-slate-900 p-3 font-mono text-xs text-rose-200"
            >
              {run.stderr}
            </pre>
          )}
        </>
      )}
      {run.truncated && (
        <p className="text-xs text-slate-500">La sortie a été coupée : 64 Kio au plus.</p>
      )}
    </section>
  )
}

/** Une phrase seulement quand le programme ne s'est pas terminé normalement. */
function Outcome({ run }: { run: ProgramRun }) {
  const problem =
    run.status === 'COMPILATION_ERROR'
      ? 'Le code ne compile pas.'
      : run.status === 'TIMEOUT'
        ? 'Le programme ne s’est pas arrêté à temps : une boucle infinie ?'
        : run.exitCode !== 0
          ? `Le programme s’est arrêté avec le code ${run.exitCode}.`
          : undefined
  return problem ? <p className="font-medium text-rose-700">{problem}</p> : null
}
