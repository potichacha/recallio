import ReactMarkdown from 'react-markdown'
import rehypeHighlight from 'rehype-highlight'

const REHYPE_PLUGINS = [rehypeHighlight]

/**
 * Affiche le texte d'une carte (Markdown : gras, code, blocs de code). react-markdown n'interprète pas le HTML brut :
 * aucun risque d'injection de script depuis le contenu.
 *
 * Les blocs de code sont colorés selon le langage annoncé par le document (```go, ```java...), parmi les langages
 * courants de highlight.js ; un langage inconnu ou absent reste en texte brut. Les couleurs sont dans index.css.
 */
export function Markdown({ children }: { children: string }) {
  return (
    <div className="prose prose-slate max-w-none prose-pre:bg-slate-900 prose-pre:text-slate-100 prose-code:before:content-none prose-code:after:content-none">
      <ReactMarkdown rehypePlugins={REHYPE_PLUGINS}>{children}</ReactMarkdown>
    </div>
  )
}
