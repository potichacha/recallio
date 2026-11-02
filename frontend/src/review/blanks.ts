/** Le marqueur d'un trou dans la question d'un code à trous (ADR 0027), comme côté serveur. */
const BLANK_MARKER = '___'

/**
 * Numérote les trous de la question (`___` devient `[1]`, `[2]`...), pour les relier aux cases de saisie. Le
 * remplacement vaut aussi dans les blocs de code, que le Markdown affiche tels quels.
 */
export function numberBlanks(prompt: string): string {
  let index = 0
  return prompt.split(BLANK_MARKER).reduce((text, part) => `${text}[${++index}]${part}`)
}
