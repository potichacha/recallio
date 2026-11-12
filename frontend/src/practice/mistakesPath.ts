/** L'adresse du carnet d'erreurs d'un sujet (ADR 0057). */
export function mistakesPath(subject: string): string {
  return `/subjects/${encodeURIComponent(subject)}/mistakes`
}
