import type { ReportReason } from '../api/reports'

/** Ce qui peut ne pas aller dans une carte ou une page (ADR 0042), dans l'ordre où on le propose. */
export const REPORT_REASONS: { reason: ReportReason; label: string }[] = [
  { reason: 'WRONG_ANSWER', label: 'La réponse est fausse' },
  { reason: 'UNCLEAR', label: 'Ce n’est pas clair' },
  { reason: 'BROKEN_CODE', label: 'Le code ne marche pas' },
  { reason: 'TYPO', label: 'Une faute de frappe' },
  { reason: 'OTHER', label: 'Autre chose' },
]

export const REPORT_REASON_LABELS = Object.fromEntries(
  REPORT_REASONS.map(({ reason, label }) => [reason, label]),
) as Record<ReportReason, string>
