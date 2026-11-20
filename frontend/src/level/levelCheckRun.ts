/**
 * Deux domaines ratés de suite arrêtent le test de niveau (ADR 0068) : un débutant a fini en quatre questions, un
 * connaisseur en vingt-quatre au plus. Les domaines qui ne sont pas posés ne sont pas connus.
 */
export const FAILED_DOMAINS_TO_STOP = 2

/** Le verdict d'un domaine posé pendant ce test. */
export type DomainVerdict = { domain: string; passed: boolean }

/** Vrai quand le test est fini : tous les domaines sont posés, ou les derniers ont été ratés de suite. */
export function isOver(verdicts: DomainVerdict[], domainCount: number): boolean {
  if (verdicts.length >= domainCount) return true
  const last = verdicts.slice(-FAILED_DOMAINS_TO_STOP)
  return last.length === FAILED_DOMAINS_TO_STOP && last.every((verdict) => !verdict.passed)
}

/** « 9 notions », « 1 notion ». */
export function notionCount(count: number): string {
  return `${count} notion${count > 1 ? 's' : ''}`
}
