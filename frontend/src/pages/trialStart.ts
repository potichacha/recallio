/**
 * Où reprendre un essai commencé sur la page publique (ADR 0058) : le nombre de questions déjà répondues, et combien
 * l'étaient juste. Passé dans l'état de la navigation ; absent, l'essai commence à sa première question.
 */
export type TrialStart = { answered: number; right: number }

export function trialStart(state: unknown): TrialStart {
  const start = state as Partial<TrialStart> | null
  return typeof start?.answered === 'number' && typeof start.right === 'number'
    ? { answered: start.answered, right: start.right }
    : { answered: 0, right: 0 }
}
