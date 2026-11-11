import ReactMarkdown, { type Options as MarkdownOptions } from 'react-markdown'
import rehypeHighlight, { type Options } from 'rehype-highlight'
import { HIGHLIGHT_LANGUAGES } from './highlightLanguages'

const HIGHLIGHT: Options = { languages: HIGHLIGHT_LANGUAGES }
const REHYPE_PLUGINS: MarkdownOptions['rehypePlugins'] = [[rehypeHighlight, HIGHLIGHT]]

/**
 * Affiche le texte d'une carte (Markdown : gras, code, blocs de code). react-markdown n'interprète pas le HTML brut :
 * aucun risque d'injection de script depuis le contenu.
 *
 * Les blocs de code sont colorés selon le langage annoncé par le document (```go, ```java...), parmi ceux de
 * highlightLanguages.ts ; un langage inconnu ou absent reste en texte brut. Les couleurs sont dans index.css.
 */
export function Markdown({ children }: { children: string }) {
  return (
    <div className="prose max-w-none prose-code:before:content-none prose-code:after:content-none">
      <ReactMarkdown rehypePlugins={REHYPE_PLUGINS}>{children}</ReactMarkdown>
    </div>
  )
}
