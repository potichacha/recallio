import { useState } from 'react'
import { useLocation } from 'react-router'
import { ApiError } from '../api/errors'
import { useCheckTourTask, type TourTask } from '../api/tour'
import { useSession } from '../auth/session'
import { Markdown } from '../components/Markdown'
import { ExerciseEditor } from '../review/ExerciseEditor'
import { ProgramOutput } from './ProgramOutput'
import { button } from '../components/ui/button'

/**
 * L'« À toi » d'une page du Tour (ADR 0037) : une consigne et un code de départ à modifier. « Vérifier mon programme »
 * l'exécute sur le serveur, qui compare sa sortie à celle attendue sans la dévoiler. Rien n'est enregistré.
 */
export function TaskPanel({
  subject,
  notion,
  page,
  task,
}: {
  subject: string
  notion: string
  page: number
  task: TourTask
}) {
  const session = useSession()
  const location = useLocation()
  const [edited, setEdited] = useState(task.starter)
  // L'éditeur ne lit son contenu qu'à sa création : le recréer est la seule façon de rendre le code de départ.
  const [version, setVersion] = useState(0)
  const check = useCheckTourTask(subject, notion)
  const headingId = `task-${notion}-${page}`

  const edit = (path: string, content: string) =>
    setEdited((current) => current.map((file) => (file.path === path ? { path, content } : file)))

  const reset = () => {
    setEdited(task.starter)
    setVersion((current) => current + 1)
    check.reset()
  }

  return (
    <section
      aria-labelledby={headingId}
      className="flex flex-col gap-3 rounded-xl border border-brand-line bg-surface p-5 shadow-sm"
    >
      <h2 id={headingId} className="text-lg font-semibold">
        À toi
      </h2>
      <Markdown>{task.instructions}</Markdown>
      <ExerciseEditor
        key={version}
        label="Fichiers de l’« À toi »"
        files={edited}
        tests={[]}
        readOnly={false}
        onEdit={edit}
      />
      <div className="flex flex-wrap gap-2">
        {session.status === 'authenticated' ? (
          <button
            type="button"
            disabled={check.isPending}
            onClick={() => check.mutate({ page, files: edited })}
            className={button()}
          >
            {check.isPending ? 'Vérification…' : 'Vérifier mon programme'}
          </button>
        ) : (
          <button
            type="button"
            onClick={() => session.login(location.pathname + location.search)}
            className={button()}
          >
            Se connecter pour vérifier
          </button>
        )}
        <button
          type="button"
          onClick={reset}
          className="rounded-lg border border-line px-4 py-2 font-medium text-ink-soft hover:bg-surface-muted"
        >
          Réinitialiser
        </button>
      </div>
      {check.error && (
        <p role="alert" className="text-sm text-danger">
          {check.error instanceof ApiError ? check.error.message : 'La vérification a échoué.'}
        </p>
      )}
      {check.data && (
        <>
          <p
            role="status"
            className={`font-medium ${check.data.passed ? 'text-success' : 'text-danger'}`}
          >
            {check.data.passed
              ? 'Bravo : c’est la sortie attendue !'
              : 'Pas encore : la sortie ne correspond pas à celle attendue.'}
          </p>
          <ProgramOutput run={check.data.run} />
        </>
      )}
    </section>
  )
}
