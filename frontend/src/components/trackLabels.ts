import type { LearningTrack } from '../api/profile'

/** Les parcours d'un sujet suivi (ADR 0032), dans l'ordre où on les propose. */
export const TRACKS: { track: LearningTrack; label: string; description: string }[] = [
  {
    track: 'LEARN',
    label: 'Apprendre',
    description:
      'Du débutant vers l’expert : une notion se débloque quand ses prérequis sont acquis.',
  },
  {
    track: 'PREPARE',
    label: 'Préparer un entretien',
    description: 'Les notions les plus essentielles d’abord, sans attendre les prérequis.',
  },
  {
    track: 'MAINTAIN',
    label: 'Entretenir son niveau',
    description: 'Les notions intermédiaires et expertes d’abord ; les bases en dernier.',
  },
]
