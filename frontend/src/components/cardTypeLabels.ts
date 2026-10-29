import type { CardType } from '../api/content'

/** Libellé affiché au-dessus d'une carte, selon son format. */
export const CARD_TYPE_LABELS: Record<CardType, string> = {
  FLASHCARD: 'Question',
  CODE_OUTPUT: "Qu'affiche ce code ?",
  OPEN_SCENARIO: 'Mise en situation',
  DEFINITION: 'Définition',
}
