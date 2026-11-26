/**
 * La carte en relief des maquettes « trois portes » (ADR 0077) : un fond de surface, des coins de 24 px et un bord
 * épais en dessous, sans ombre floue. Elle ne se presse pas : une carte qui se clique reste un lien ou un bouton, avec
 * son focus.
 */
export function card(): string {
  return 'rounded-2xl bg-surface p-4.5 shadow-relief'
}
