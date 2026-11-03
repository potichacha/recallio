import { java } from '@codemirror/lang-java'
import { Compartment, EditorState } from '@codemirror/state'
import { EditorView } from '@codemirror/view'
import { basicSetup } from 'codemirror'
import { useEffect, useRef } from 'react'

export type CodeEditorProps = {
  /** Le nom accessible de la zone de saisie : le chemin du fichier. */
  label: string
  /** Le contenu initial ; l'éditeur garde ensuite le sien, et le signale par `onChange`. */
  value: string
  readOnly?: boolean
  onChange?: (value: string) => void
}

const theme = EditorView.theme({
  '&': { fontSize: '0.875rem' },
  '.cm-content, .cm-gutters': { fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace' },
  '.cm-scroller': { maxHeight: '28rem' },
})

/**
 * Un éditeur de code Java (CodeMirror 6, ADR 0030) : coloration, numéros de ligne, indentation, annulation. Chargé à la
 * demande (export par défaut, pour `React.lazy`) : seules les pages d'exercice en paient le poids.
 *
 * Le contenu n'est lu qu'à la création : l'éditeur reste maître du texte (curseur, historique d'annulation), et passer
 * en lecture seule ne le recrée pas.
 */
export default function CodeEditor({ label, value, readOnly = false, onChange }: CodeEditorProps) {
  const host = useRef<HTMLDivElement>(null)
  const view = useRef<EditorView>(null)
  const editable = useRef(new Compartment())
  const initial = useRef({ value, readOnly })
  const changed = useRef(onChange)

  useEffect(() => {
    changed.current = onChange
  })

  useEffect(() => {
    if (!host.current) return
    const created = new EditorView({
      parent: host.current,
      state: EditorState.create({
        doc: initial.current.value,
        extensions: [
          basicSetup,
          java(),
          theme,
          editable.current.of(editability(initial.current.readOnly)),
          EditorView.contentAttributes.of({ 'aria-label': label }),
          EditorView.updateListener.of((update) => {
            if (update.docChanged) changed.current?.(update.state.doc.toString())
          }),
        ],
      }),
    })
    view.current = created
    return () => created.destroy()
  }, [label])

  useEffect(() => {
    view.current?.dispatch({ effects: editable.current.reconfigure(editability(readOnly)) })
  }, [readOnly])

  return <div ref={host} className="overflow-hidden rounded-lg border border-slate-300" />
}

function editability(readOnly: boolean) {
  return [EditorState.readOnly.of(readOnly), EditorView.editable.of(!readOnly)]
}
