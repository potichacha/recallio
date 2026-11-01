/** Ce qu'un choix de QCM montre une fois la réponse corrigée. */
export type ChoiceOutcome = 'correct' | 'missed' | 'wrong' | 'neutral'

export function choiceOutcome(
  correctChoices: number[],
  index: number,
  chosen: boolean,
): ChoiceOutcome {
  if (correctChoices.includes(index)) return chosen ? 'correct' : 'missed'
  return chosen ? 'wrong' : 'neutral'
}

/** La sélection d'un QCM : un seul choix pour des boutons radio, plusieurs pour des cases à cocher. */
export function toggleChoice(current: number[], index: number, multiple: boolean): number[] {
  if (!multiple) return [index]
  return current.includes(index) ? current.filter((other) => other !== index) : [...current, index]
}
