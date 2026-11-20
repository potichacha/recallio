import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type DailyChallenge = components['schemas']['DailyChallenge']
export type DailyChallengeSummary = components['schemas']['DailyChallengeSummary']
export type DailyChallengeQuestion = components['schemas']['DailyChallengeQuestion']
export type DailyChallengeAnswered = components['schemas']['DailyChallengeAnswered']
export type DailyChallengeStats = components['schemas']['DailyChallengeStats']
export type DailyChallengeResponse = components['schemas']['DailyChallengeResponse']
export type DailyChallengeAnswerResult = components['schemas']['DailyChallengeAnswerResult']

/** Clé partagée : répondre à un défi change son état dans la liste de la page Jouer et de l'accueil. */
export const DAILY_CHALLENGES_KEY = ['daily-challenges']

const challengeKey = (subject: string) => ['daily-challenge', subject]

/** Le défi du jour de chaque sujet qui en a un, les sujets suivis d'abord (ADR 0067). */
export function useDailyChallenges(enabled = true) {
  return useQuery({
    queryKey: DAILY_CHALLENGES_KEY,
    enabled,
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/daily-challenges')
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Le défi du jour d'un sujet, tiré s'il ne l'est pas encore ; ou celui de la veille, s'il est à finir. */
export function useDailyChallenge(subject: string, enabled = true) {
  return useQuery({
    queryKey: challengeKey(subject),
    enabled,
    // Un défi en cours garde ses questions : recharger en revenant sur l'onglet ne change rien d'utile.
    refetchOnWindowFocus: false,
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/daily-challenges/{subject}', {
        params: { path: { subject } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Une réponse à une question du défi : son identifiant de requête la rend idempotente (CLAUDE.md, section 4.4). */
export type DailyChallengeAnswer = {
  challengeId: string
  requestId: string
  position: number
  answer: DailyChallengeResponse
}

/**
 * Répond à une question du défi, une fois pour toutes : la correction tout de suite. Un refus 409 (la question a déjà
 * sa réponse, depuis un autre onglet, ou le défi est fermé) recharge le défi, pour montrer où il en est vraiment.
 */
export function useAnswerDailyChallenge(subject: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ challengeId, requestId, position, answer }: DailyChallengeAnswer) => {
      const { data, error, response } = await api.POST(
        '/api/v1/me/daily-challenges/{challengeId}/answers',
        { params: { path: { challengeId } }, body: { requestId, position, answer } },
      )
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: DAILY_CHALLENGES_KEY }),
    onError: (error) => {
      if (error instanceof ApiError && error.status === 409) {
        void queryClient.invalidateQueries({ queryKey: challengeKey(subject) })
      }
    },
  })
}

/**
 * Le défi tel qu'il est après une réponse : la question répondue rejoint les réponses, avec sa correction, et le défi
 * prend l'état, le score et les chiffres du jour que le serveur vient de rendre. Évite de recharger le défi entre deux
 * questions.
 */
export function withAnswer(
  challenge: DailyChallenge,
  result: DailyChallengeAnswerResult,
): DailyChallenge {
  const { correction } = result
  const answered: DailyChallengeAnswered = {
    position: correction.position,
    correct: correction.correct,
    correctChoices: correction.correctChoices,
    expectedBlanks: correction.blanks.map((blank) => blank.expected),
    explanation: correction.explanation,
  }
  return {
    ...challenge,
    state: result.state,
    score: result.score,
    questionCount: result.questionCount,
    stats: result.stats,
    answers: [
      ...challenge.answers.filter((other) => other.position !== answered.position),
      answered,
    ].sort((a, b) => a.position - b.position),
  }
}

/** Range le défi mis à jour dans le cache, pour que la page passe à la question suivante ou au résultat. */
export function useStoreDailyChallenge(subject: string) {
  const queryClient = useQueryClient()
  return (challenge: DailyChallenge) => queryClient.setQueryData(challengeKey(subject), challenge)
}
