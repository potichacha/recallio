import { go } from '@codemirror/lang-go'
import { java } from '@codemirror/lang-java'
import { HighlightStyle, syntaxHighlighting } from '@codemirror/language'
import { Compartment, EditorState } from '@codemirror/state'
import { EditorView } from '@codemirror/view'
import { tags } from '@lezer/highlight'
import { basicSetup } from 'codemirror'
import { useEffect, useRef } from 'react'

export type CodeEditorProps = {
  /** Le nom accessible de la zone de saisie : le chemin du fichier, dont l'extension choisit la coloration. */
  label: string
  /** Le contenu initial ; l'éditeur garde ensuite le sien, et le signale par `onChange`. */
  value: string
  readOnly?: boolean
  onChange?: (value: string) => void
}

/** L'éditeur sur la palette du code (ADR 0049) : sombre dans les deux thèmes, comme les blocs de code. */
const theme = EditorView.theme(
  {
    '&': {
      fontSize: '0.875rem',
      backgroundColor: 'var(--color-code)',
      color: 'var(--color-code-ink)',
    },
    '.cm-content, .cm-gutters': { fontFamily: 'var(--font-mono)' },
    '.cm-content': { caretColor: 'var(--color-code-ink)' },
    '.cm-cursor, .cm-dropCursor': { borderLeftColor: 'var(--color-code-ink)' },
    '.cm-gutters': {
      backgroundColor: 'var(--color-code)',
      color: 'var(--color-code-muted)',
      border: 'none',
    },
    '.cm-activeLine': { backgroundColor: 'rgb(255 255 255 / 0.04)' },
    '.cm-activeLineGutter': { backgroundColor: 'rgb(255 255 255 / 0.06)' },
    '&.cm-focused > .cm-scroller > .cm-selectionLayer .cm-selectionBackground, .cm-selectionBackground, .cm-content ::selection':
      { backgroundColor: 'rgb(94 207 194 / 0.28)' },
    '.cm-scroller': { maxHeight: '28rem' },
  },
  { dark: true },
)

/** La coloration syntaxique, aux couleurs des blocs de code (index.css). */
const highlight = syntaxHighlighting(
  HighlightStyle.define([
    {
      tag: [tags.keyword, tags.modifier, tags.operatorKeyword, tags.controlKeyword],
      color: 'var(--color-code-keyword)',
    },
    {
      tag: [tags.string, tags.regexp, tags.special(tags.string)],
      color: 'var(--color-code-string)',
    },
    { tag: [tags.number, tags.bool, tags.null, tags.atom], color: 'var(--color-code-number)' },
    {
      tag: [tags.typeName, tags.className, tags.function(tags.variableName)],
      color: 'var(--color-code-title)',
    },
    {
      tag: [tags.comment, tags.lineComment, tags.blockComment],
      color: 'var(--color-code-comment)',
      fontStyle: 'italic',
    },
    { tag: tags.invalid, color: 'var(--color-code-error)' },
  ]),
)

/** La coloration d'un fichier, d'après son extension (ADR 0033) : Go pour un `.go`, Java sinon. */
function language(path: string) {
  return path.endsWith('.go') ? go() : java()
}

/**
 * Un éditeur de code Java ou Go (CodeMirror 6, ADR 0030, 0033) : coloration, numéros de ligne, indentation, annulation.
 * Chargé à la demande (export par défaut, pour `React.lazy`) : seules les pages d'exercice en paient le poids.
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
          language(label),
          theme,
          highlight,
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

  return <div ref={host} className="overflow-hidden rounded-lg border border-line-strong" />
}

function editability(readOnly: boolean) {
  return [EditorState.readOnly.of(readOnly), EditorView.editable.of(!readOnly)]
}
