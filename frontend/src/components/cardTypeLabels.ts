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

/** Les formats corrigés par le serveur d'après les choix cochés (ADR 0019, 0027) ; les autres s'auto-évaluent. */
export const GRADED_BY_CHOICES: ReadonlySet<CardType> = new Set(['MULTIPLE_CHOICE', 'TRUE_FALSE'])

/**
 * Tous les formats que le serveur corrige : des choix cochés, des trous remplis, ou des tests qui passent. Ce sont ceux
 * du mode « questions corrigées » d'une session (ADR 0048).
 */
export const SERVER_GRADED_TYPES: readonly CardType[] = [
  'MULTIPLE_CHOICE',
  'TRUE_FALSE',
  'FILL_IN',
  'CODING_EXERCISE',
]
