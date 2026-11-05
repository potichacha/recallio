import { useState } from 'react'
import { useLocation } from 'react-router'
import { ApiError } from '../api/errors'
import { useRunProgram, type ProgramFile } from '../api/tour'
import { useSession } from '../auth/session'
import { ExerciseEditor } from '../review/ExerciseEditor'
import { ProgramOutput } from './ProgramOutput'
import { button } from '../components/ui/button'

/**
 * Le programme d'une page du Tour (ADR 0037) : on le modifie, on l'exécute dans le bac à sable, on lit ce qu'il écrit.
 * « Réinitialiser » rend le programme de la page. Lire le Tour est public ; exécuter demande d'être connecté.
 */
export function Playground({ files }: { files: ProgramFile[] }) {
  const session = useSession()
  const location = useLocation()
  const [edited, setEdited] = useState(files)
  // L'éditeur ne lit son contenu qu'à sa création : le recréer est la seule façon de rendre le programme de la page.
  const [version, setVersion] = useState(0)
  const run = useRunProgram()

  const edit = (path: string, content: string) =>
    setEdited((current) => current.map((file) => (file.path === path ? { path, content } : file)))

  const reset = () => {
    setEdited(files)
    setVersion((current) => current + 1)
    run.reset()
  }

  return (
    <section aria-label="Programme" className="flex flex-col gap-3">
      <ExerciseEditor
        key={version}
        label="Fichiers du programme"
        files={edited}
        tests={[]}
        readOnly={false}
        onEdit={edit}
      />
      <div className="flex flex-wrap gap-2">
        {session.status === 'authenticated' ? (
          <button
            type="button"
            disabled={run.isPending}
            onClick={() => run.mutate(edited)}
            className={button()}
          >
            {run.isPending ? 'Exécution…' : 'Exécuter'}
          </button>
        ) : (
          <button
            type="button"
            onClick={() => session.login(location.pathname + location.search)}
            className={button()}
          >
            Se connecter pour exécuter
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
      {run.error && (
        <p role="alert" className="text-sm text-danger">
          {run.error instanceof ApiError ? run.error.message : 'L’exécution a échoué.'}
        </p>
      )}
      {run.data && <ProgramOutput run={run.data} />}
    </section>
  )
}
