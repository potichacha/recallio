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

/** Un choix à afficher, avec sa position parmi les choix reçus : celle que le serveur corrige. */
export type DisplayedChoice = { index: number; text: string }

/** Les deux choix d'un vrai ou faux (ADR 0027) : fixes, ils gardent leur ordre. */
const TRUE_FALSE_CHOICES = ['Vrai', 'Faux']

/**
 * Les choix dans leur ordre d'affichage (ADR 0051) : mélangés, pour que la bonne réponse ne soit pas toujours à la
 * même place ; un vrai ou faux garde le sien.
 */
export function shuffleChoices(
  choices: readonly string[],
  random: () => number = Math.random,
): DisplayedChoice[] {
  const displayed = choices.map((text, index) => ({ index, text }))
  const trueFalse =
    choices.length === TRUE_FALSE_CHOICES.length &&
    choices.every((choice, index) => choice === TRUE_FALSE_CHOICES[index])
  if (trueFalse) return displayed
  return displayed
    .map((choice) => ({ choice, key: random() }))
    .sort((a, b) => a.key - b.key)
    .map(({ choice }) => choice)
}
