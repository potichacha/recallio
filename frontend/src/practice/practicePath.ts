/**
 * Le lien de l'entraînement sur une notion (ADR 0038).
 *
 * @param from `tour` y ajoute un retour à la page du Tour
 * @param card la carte par laquelle commencer, par exemple un exercice de fin de chapitre
 */
export function practicePath(
  subject: string,
  notion: string,
  { from, card }: { from?: 'tour'; card?: string } = {},
): string {
  const base = `/subjects/${encodeURIComponent(subject)}/practice/${encodeURIComponent(notion)}`
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (card) params.set('card', card)
  const query = params.toString()
  return query ? `${base}?${query}` : base
}
