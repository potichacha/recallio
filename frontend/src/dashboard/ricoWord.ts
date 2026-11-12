import type { Progress } from '../api/progress'
import type { RicoMood } from '../components/Rico'

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

export type RicoWord = { greeting: string; message: string; mood: RicoMood }

/**
 * Ce que Rico dit en tête de l'accueil (ADR 0058) : un salut, puis une phrase choisie d'après la journée. Une fonction
 * pure, sans hasard : la même situation donne la même phrase, et chaque cas se teste.
 *
 * @param newcomer vrai tant qu'aucune carte n'a été révisée : on accueille, sans parler d'une série à zéro
 * @param nothingToReview vrai si le sujet en cours n'a plus ni carte due ni nouvelle carte aujourd'hui
 */
export function ricoWord({
  firstName,
  progress,
  newcomer,
  nothingToReview,
}: {
  firstName?: string
  progress: Progress
  newcomer: boolean
  nothingToReview: boolean
}): RicoWord {
  const name = firstName ? ` ${firstName}` : ''
  if (newcomer) {
    return {
      greeting: `Bienvenue${name} !`,
      message: 'Ta première session dure dix minutes. On y va ?',
      mood: 'happy',
    }
  }
  const greeting = `Salut${name} !`
  const { cardsToday, dailyGoal, goalReachedToday, streak } = progress
  if (goalReachedToday) {
    return {
      greeting,
      message: `Objectif atteint : ${plural(streak, 'jour')} de suite. À demain !`,
      mood: 'happy',
    }
  }
  if (nothingToReview) {
    return {
      greeting,
      message: 'Tout est à jour ici pour le moment. Ouvre un autre sujet, ou reviens plus tard.',
      mood: 'thinking',
    }
  }
  const left = plural(Math.max(1, dailyGoal - cardsToday), 'carte')
  return {
    greeting,
    message:
      streak > 0
        ? `Encore ${left} et ta série passe à ${plural(streak + 1, 'jour')}.`
        : `Encore ${left} pour lancer ta série.`,
    mood: 'cheering',
  }
}
