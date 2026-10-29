import ReactMarkdown from 'react-markdown'

/**
 * Affiche le texte d'une carte (Markdown : gras, code, blocs de code). react-markdown n'interprète pas le HTML brut :
 * aucun risque d'injection de script depuis le contenu.
 */
export function Markdown({ children }: { children: string }) {
  return (
    <div className="prose prose-slate max-w-none prose-pre:bg-slate-900 prose-pre:text-slate-100 prose-code:before:content-none prose-code:after:content-none">
      <ReactMarkdown>{children}</ReactMarkdown>
    </div>
  )
}
