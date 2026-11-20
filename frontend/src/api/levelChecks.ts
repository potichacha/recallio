import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type LevelCheck = components['schemas']['LevelCheck']
export type LevelCheckDomain = components['schemas']['LevelCheckDomain']
export type LevelCheckQuestion = components['schemas']['LevelCheckQuestion']
export type LevelCheckAnswer = components['schemas']['LevelCheckAnswer']
export type LevelCheckDomainResult = components['schemas']['LevelCheckDomainResult']

const levelCheckKey = (subject: string) => ['level-check', subject]

/**
 * Ce que le test de niveau change ailleurs : les notions connues du sommaire du Tour, l'état des notions dans le cours,
 * et les nouvelles cartes de la file de révision.
 */
function invalidateProgress(queryClient: ReturnType<typeof useQueryClient>, subject: string) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: ['tour-progress', subject] }),
    queryClient.invalidateQueries({ queryKey: ['learning-path', subject] }),
    queryClient.invalidateQueries({ queryKey: ['review-queue'] }),
  ])
}

/**
 * Le test de niveau d'un sujet (ADR 0068), tiré à chaque visite de la page : ses domaines des bases et leurs questions.
 * Il n'est pas rechargé pendant qu'on le passe, ce qui en tirerait d'autres questions, ni gardé une fois la page
 * quittée : y revenir tire un nouveau test.
 */
export function useLevelCheck(subject: string) {
  return useQuery({
    queryKey: levelCheckKey(subject),
    staleTime: Infinity,
    gcTime: 0,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/level-checks/{subject}', {
        params: { path: { subject } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Corrige les réponses aux questions d'un domaine ; toutes justes, ses notions débutantes deviennent connues. */
export function useCheckLevelCheckDomain(subject: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ domain, answers }: { domain: string; answers: LevelCheckAnswer[] }) => {
      const { data, error, response } = await api.POST(
        '/api/v1/me/level-checks/{subject}/domains/{domain}',
        { params: { path: { subject, domain } }, body: { answers } },
      )
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: (result) => (result.passed ? invalidateProgress(queryClient, subject) : undefined),
  })
}

/** Annule le test d'un sujet : ses notions connues sont oubliées, et un nouveau test est tiré. */
export function useCancelLevelCheck(subject: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      const { error, response } = await api.DELETE('/api/v1/me/level-checks/{subject}', {
        params: { path: { subject } },
      })
      if (!response.ok) throw ApiError.from(response, error)
    },
    onSuccess: () =>
      Promise.all([
        invalidateProgress(queryClient, subject),
        queryClient.invalidateQueries({ queryKey: levelCheckKey(subject) }),
      ]),
  })
}
