import type { CardType } from '../api/content'

/** Libellé affiché au-dessus d'une carte, selon son format. */
export const CARD_TYPE_LABELS: Record<CardType, string> = {
  FLASHCARD: 'Question',
  CODE_OUTPUT: "Qu'affiche ce code ?",
  OPEN_SCENARIO: 'Mise en situation',
  DEFINITION: 'Définition',
  MULTIPLE_CHOICE: 'QCM',
  TRUE_FALSE: 'Vrai ou faux',
  BUG_FIX: 'Bug à corriger',
  FILL_IN: 'Compléter le code',
  CODING_EXERCISE: 'Exercice de code',
}
