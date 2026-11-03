import { lazy, Suspense, useId, useState } from 'react'
import type { ExerciseFile } from '../api/review'

const CodeEditor = lazy(() => import('../components/CodeEditor'))

type Tab = { file: ExerciseFile; editable: boolean }

/**
 * Les fichiers d'un exercice, un onglet chacun : ceux du code de départ se modifient, les tests se lisent. Chaque
 * éditeur reste monté quand on change d'onglet : le texte, le curseur et l'historique d'annulation sont gardés.
 */
export function ExerciseEditor({
  files,
  tests,
  readOnly,
  onEdit,
}: {
  files: ExerciseFile[]
  tests: ExerciseFile[]
  readOnly: boolean
  onEdit: (path: string, content: string) => void
}) {
  const id = useId()
  const tabs: Tab[] = [
    ...files.map((file) => ({ file, editable: true })),
    ...tests.map((file) => ({ file, editable: false })),
  ]
  const [selected, setSelected] = useState(tabs[0]?.file.path)

  return (
    <div className="flex flex-col gap-2">
      <div role="tablist" aria-label="Fichiers de l'exercice" className="flex flex-wrap gap-1">
        {tabs.map(({ file, editable }, index) => (
          <button
            key={file.path}
            type="button"
            role="tab"
            id={`${id}-tab-${index}`}
            aria-selected={file.path === selected}
            aria-controls={`${id}-panel-${index}`}
            onClick={() => setSelected(file.path)}
            className={`rounded-t-lg border-b-2 px-3 py-1.5 font-mono text-xs ${
              file.path === selected
                ? 'border-indigo-600 text-indigo-700'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            {file.path}
            {!editable && <span className="ml-1 font-sans text-slate-400">(test)</span>}
          </button>
        ))}
      </div>
      {tabs.map(({ file, editable }, index) => (
        <div
          key={file.path}
          role="tabpanel"
          id={`${id}-panel-${index}`}
          aria-labelledby={`${id}-tab-${index}`}
          hidden={file.path !== selected}
        >
          <Suspense fallback={<p className="text-sm text-slate-500">Chargement de l’éditeur…</p>}>
            <CodeEditor
              label={file.path}
              value={file.content}
              readOnly={readOnly || !editable}
              onChange={editable ? (content) => onEdit(file.path, content) : undefined}
            />
          </Suspense>
        </div>
      ))}
    </div>
  )
}
