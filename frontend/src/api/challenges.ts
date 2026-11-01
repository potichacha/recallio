import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type ChallengeSummary = components['schemas']['ChallengeSummary']
export type Challenge = components['schemas']['Challenge']
export type ChallengeStep = components['schemas']['ChallengeStep']
export type ChallengeCard = components['schemas']['ChallengeCard']
export type ChallengeAnswerResult = components['schemas']['ChallengeAnswerResult']
export type CreateChallengeRequest = components['schemas']['CreateChallengeRequest']

const groupChallengesKey = (groupId: string) => ['groups', groupId, 'challenges']
const challengeKey = (challengeId: string) => ['challenges', challengeId]

/** Les défis récents d'un groupe. */
export function useGroupChallenges(groupId: string) {
  return useQuery({
    queryKey: groupChallengesKey(groupId),
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/groups/{groupId}/challenges', {
        params: { path: { groupId } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Le créateur lance un défi. */
export function useCreateChallenge(groupId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (request: CreateChallengeRequest) => {
      const { data, error, response } = await api.POST('/api/v1/groups/{groupId}/challenges', {
        params: { path: { groupId } },
        body: request,
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: groupChallengesKey(groupId) }),
  })
}

/** Le défi, la partie de l'utilisateur et le classement. */
export function useChallenge(challengeId: string) {
  return useQuery({
    queryKey: challengeKey(challengeId),
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/challenges/{challengeId}', {
        params: { path: { challengeId } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    retry: (failureCount, error) =>
      failureCount < 3 && !(error instanceof ApiError && error.status < 500),
  })
}

/**
 * La carte à jouer. Le serveur décide : la carte en cours si son délai court encore, sinon la suivante. Demander deux
 * fois la même ne relance pas le chrono.
 */
export function useNextChallengeCard(challengeId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      const { data, error, response } = await api.POST('/api/v1/challenges/{challengeId}/next', {
        params: { path: { challengeId } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: async (step) => {
      // La partie terminée, le défi (statut, score, classement) a changé.
      if (step.finished)
        await queryClient.invalidateQueries({ queryKey: challengeKey(challengeId) })
    },
  })
}

export function useAnswerChallengeCard(challengeId: string) {
  return useMutation({
    mutationFn: async ({
      position,
      selectedChoices,
    }: {
      position: number
      selectedChoices: number[]
    }) => {
      const { data, error, response } = await api.POST('/api/v1/challenges/{challengeId}/answers', {
        params: { path: { challengeId } },
        body: { position, selectedChoices },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}
