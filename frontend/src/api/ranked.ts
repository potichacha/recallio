import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type RankedOverview = components['schemas']['RankedOverview']
export type SubjectRank = components['schemas']['SubjectRank']
export type Rank = components['schemas']['Rank']
export type Tier = Rank['tier']
export type Duel = components['schemas']['Duel']
export type DuelTurn = components['schemas']['DuelTurn']
export type DuelQuestion = components['schemas']['DuelQuestion']
export type DuelAnswerResult = components['schemas']['DuelAnswerResult']
export type DuelSummary = components['schemas']['DuelSummary']

const RANKED_KEY = ['ranked']
const duelKey = (id: string) => [...RANKED_KEY, 'duel', id]

/** Le rang de chaque sujet suivi, le placement et les insignes de saison (ADR 0046). */
export function useRankedOverview() {
  return useQuery({
    queryKey: [...RANKED_KEY, 'overview'],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/ranked')
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

export function useMyDuels() {
  return useQuery({
    queryKey: [...RANKED_KEY, 'duels'],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/ranked/duels')
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/**
 * Un duel. En attente de l'adversaire, il se relit régulièrement : souvent en direct, de loin en loin en asynchrone.
 */
export function useDuel(id: string) {
  return useQuery({
    queryKey: duelKey(id),
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/ranked/duels/{id}', {
        params: { path: { id } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    refetchInterval: (query) => {
      const duel = query.state.data
      if (!duel || duel.status === 'FINISHED' || duel.status === 'EXPIRED') return false
      return duel.mode === 'LIVE' ? 2_000 : 15_000
    },
  })
}

/** Cherche un duel asynchrone : le sien, un duel ouvert, ou un nouveau. */
export function useFindDuel() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (subject: string) => {
      const { data, error, response } = await api.POST('/api/v1/ranked/duels', {
        body: { subject },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [...RANKED_KEY, 'duels'] }),
  })
}

/** Entre dans la file du direct, ou s'y signale : à rappeler toutes les quelques secondes. */
export function useQueueLive() {
  return useMutation({
    mutationFn: async (subject: string) => {
      const { data, error, response } = await api.POST('/api/v1/ranked/live-queue', {
        body: { subject },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

export function useLeaveLive() {
  return useMutation({
    mutationFn: async () => {
      const { error, response } = await api.DELETE('/api/v1/ranked/live-queue')
      if (!response.ok) throw ApiError.from(response, error)
    },
  })
}

/** La question suivante, servie une seule fois par le serveur. */
export function useNextQuestion(id: string) {
  return useMutation({
    mutationFn: async () => {
      const { data, error, response } = await api.POST('/api/v1/ranked/duels/{id}/next', {
        params: { path: { id } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

export function useAnswerDuel(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (answer: components['schemas']['DuelAnswerRequest']) => {
      const { data, error, response } = await api.POST('/api/v1/ranked/duels/{id}/answers', {
        params: { path: { id } },
        body: answer,
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: (result) => {
      if (result.finished) return queryClient.invalidateQueries({ queryKey: duelKey(id) })
    },
  })
}
