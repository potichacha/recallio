import bash from 'highlight.js/lib/languages/bash'
import c from 'highlight.js/lib/languages/c'
import cpp from 'highlight.js/lib/languages/cpp'
import csharp from 'highlight.js/lib/languages/csharp'
import dockerfile from 'highlight.js/lib/languages/dockerfile'
import go from 'highlight.js/lib/languages/go'
import java from 'highlight.js/lib/languages/java'
import kotlin from 'highlight.js/lib/languages/kotlin'
import markdown from 'highlight.js/lib/languages/markdown'
import protobuf from 'highlight.js/lib/languages/protobuf'
import python from 'highlight.js/lib/languages/python'
import rust from 'highlight.js/lib/languages/rust'
import shell from 'highlight.js/lib/languages/shell'
import sql from 'highlight.js/lib/languages/sql'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import yaml from 'highlight.js/lib/languages/yaml'

/**
 * Les langages que la coloration du code connaît : ceux des blocs de code du contenu, et ceux des langages annoncés au
 * catalogue. highlight.js en charge 37 par défaut, bien plus qu'il n'en faut (audit de novembre 2026, élément 13).
 *
 * Un bloc de code d'un autre langage resterait en texte brut : CodeLanguagesTest (backend, job content) lit ce fichier
 * et échoue si le contenu en annonce un qui n'est pas ici. Pour ajouter un langage : une ligne d'import, une entrée.
 */
export const HIGHLIGHT_LANGUAGES = {
  bash,
  c,
  cpp,
  csharp,
  dockerfile,
  go,
  java,
  kotlin,
  markdown,
  protobuf,
  python,
  rust,
  shell,
  sql,
  typescript,
  xml,
  yaml,
}
