import { Markdown } from './Markdown'

export type SourceFile = { path: string; content: string }

/**
 * Les fichiers d'une partie d'un exercice de code (ADR 0029), en lecture seule et colorés : chacun sous son chemin. La
 * clôture de quatre accents graves laisse passer les blocs de trois qu'un commentaire pourrait contenir.
 */
export function SourceFiles({ title, files }: { title: string; files: SourceFile[] }) {
  return (
    <section aria-label={title} className="flex flex-col gap-2">
      <h3 className="text-sm font-medium text-slate-700">{title}</h3>
      {files.map((file) => (
        <figure key={file.path} className="flex flex-col gap-1">
          <figcaption className="font-mono text-xs text-slate-500">{file.path}</figcaption>
          <Markdown>{'````java\n' + file.content + '\n````'}</Markdown>
        </figure>
      ))}
    </section>
  )
}
