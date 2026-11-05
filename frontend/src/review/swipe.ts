import type { Rating } from '../api/review'

/** La distance, en pixels, à partir de laquelle glisser une carte vaut une réponse (ADR 0050). */
export const SWIPE_THRESHOLD = 96

/** Glisser à droite : « Je savais » ; à gauche : « Je ne savais pas » ; trop peu : rien, la carte revient. */
export function swipeRating(distance: number): Rating | undefined {
  if (distance >= SWIPE_THRESHOLD) return 'GOOD'
  if (distance <= -SWIPE_THRESHOLD) return 'AGAIN'
  return undefined
}

/** Vrai si la touche part d'un champ de saisie : les raccourcis de la carte ne doivent pas voler la frappe. */
export function typingIn(target: EventTarget | null): boolean {
  return (
    target instanceof HTMLElement &&
    target.closest('textarea, input, select, [contenteditable="true"]') !== null
  )
}

/** Vrai si la touche part d'un bouton ou d'un lien : Espace et Entrée l'activent, la carte ne doit pas les lui prendre. */
export function onControl(target: EventTarget | null): boolean {
  return (
    target instanceof HTMLElement &&
    target.closest('button, a[href], summary, [role="button"]') !== null
  )
}
