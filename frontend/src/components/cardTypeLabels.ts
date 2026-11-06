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

/**
 * Les formats que le serveur corrige toujours : des choix cochés, des trous remplis, ou des tests qui passent. Ce sont
 * ceux du mode « questions corrigées » d'une session (ADR 0048) ; les cartes des autres formats qui ont reçu des choix
 * (ADR 0051) le rejoindront à la fin de la migration.
 */
export const SERVER_GRADED_TYPES: readonly CardType[] = [
  'MULTIPLE_CHOICE',
  'TRUE_FALSE',
  'FILL_IN',
  'CODING_EXERCISE',
]
