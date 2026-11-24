import ReactMarkdown, { type Options as MarkdownOptions } from 'react-markdown'
import rehypeHighlight, { type Options } from 'rehype-highlight'
import { HIGHLIGHT_LANGUAGES } from './highlightLanguages'

const HIGHLIGHT: Options = { languages: HIGHLIGHT_LANGUAGES }
const REHYPE_PLUGINS: MarkdownOptions['rehypePlugins'] = [[rehypeHighlight, HIGHLIGHT]]

/**
 * Le poids du texte selon son rôle sur une carte (ADR 0058) : la question d'une carte de révision se lit comme un titre
 * (`question`), celle d'une question posée dans une page comme un sous-titre (`prompt`), un choix comme un libellé ; le
 * reste (leçon, explication) garde le poids d'un texte courant.
 */
const TONES = {
  text: '',
  question:
    'prose-p:text-[1.375rem] prose-p:leading-[1.3] prose-p:font-black prose-p:text-ink prose-p:first:mt-0',
  prompt: 'prose-p:text-[1.0625rem] prose-p:leading-snug prose-p:font-extrabold prose-p:text-ink',
  choice: 'prose-p:my-0 prose-p:leading-snug prose-p:font-extrabold prose-p:text-ink',
} as const

/**
 * Affiche le texte d'une carte (Markdown : gras, code, blocs de code). react-markdown n'interprète pas le HTML brut :
 * aucun risque d'injection de script depuis le contenu.
 *
 * Les blocs de code sont colorés selon le langage annoncé par le document (```go, ```java...), parmi ceux de
 * highlightLanguages.ts ; un langage inconnu ou absent reste en texte brut. Les couleurs sont dans index.css.
 */
export function Markdown({
  children,
  tone = 'text',
}: {
  children: string
  tone?: keyof typeof TONES
}) {
  return (
    <div
      className={`prose max-w-none prose-code:before:content-none prose-code:after:content-none ${TONES[tone]}`}
    >
      <ReactMarkdown rehypePlugins={REHYPE_PLUGINS}>{children}</ReactMarkdown>
    </div>
  )
}
