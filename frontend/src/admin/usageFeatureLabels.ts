import type { UsageFeature } from '../api/beta'

/** Le nom de chaque fonctionnalité mesurée par la page Bêta (ADR 0076), tel que l'application la nomme ailleurs. */
export const USAGE_FEATURE_LABELS: Record<UsageFeature, string> = {
  REVIEW: 'Révision',
  TOUR: 'Le Tour',
  CODING_EXERCISE: 'Exercice de code',
  MOCK_EXAM: 'Examen blanc',
  DAILY_CHALLENGE: 'Défi du jour',
  RANKED_DUEL: 'Duel classé',
  LIVE_GAME: 'Partie en direct',
  GROUP_CHALLENGE: 'Défi de groupe',
  QUEST: 'Quête terminée',
  LEVEL_CHECK: 'Test de niveau',
  INTENSIVE_DRILL: 'Révision intensive',
  ACCESS_CODE: 'Code d’accès utilisé',
  INTERVIEW_COUNTDOWN: 'Compte à rebours avant l’entretien',
  REMINDERS: 'Rappels',
  PUBLIC_PROFILE: 'Profil public',
  GROUP: 'Membre d’un groupe',
  PLUS: 'Recallio Plus',
}
