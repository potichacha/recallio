import type { DailyChallenge } from '../api/dailyChallenges'
import { fullDate } from '../components/interviewDate'
import type { ShareCard } from '../components/shareCard'

/** L'adresse du défi d'un sujet, à partager : elle ouvre le défi du jour de celui qui la reçoit. */
export function dailyChallengePath(subject: string): string {
  return `/daily/${encodeURIComponent(subject)}`
}

/** Les réussites, dans l'ordre des questions : de quoi dessiner les marques de la carte de partage. */
export function marksOf(challenge: DailyChallenge): boolean[] {
  const answers = new Map(challenge.answers.map((answer) => [answer.position, answer.correct]))
  return challenge.questions.map((question) => answers.get(question.position) ?? false)
}

/**
 * La carte de partage d'un défi fini (ADR 0045, 0067) : le score, le jour et les marques des questions, jamais leurs
 * réponses. Le texte invite à jouer le même défi.
 */
export function dailyChallengeShareCard(challenge: DailyChallenge, origin: string): ShareCard {
  const score = `${challenge.score ?? 0}/${challenge.questionCount}`
  return {
    kicker: `Défi du jour · ${challenge.subjectTitle}`,
    title: score,
    detail: fullDate(challenge.day),
    url: new URL(dailyChallengePath(challenge.subjectSlug), origin).href,
    text: `J’ai fait ${score} au défi ${challenge.subjectTitle} du jour sur Recallio. À ton tour :`,
    marks: marksOf(challenge),
  }
}
