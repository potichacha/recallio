import { useMutation, useQuery } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { PracticeAnswer } from './practice'
import type { components } from './schema'

export type TrialQuestion = components['schemas']['TrialQuestion']

/** Les questions de l'essai sans compte d'un sujet (ADR 0050) : publiques, comme le catalogue. */
export function useTrial(subject: string) {
  return useQuery({
    queryKey: ['trial', subject],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/subjects/{subject}/trial', {
        params: { path: { subject } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    // Un essai garde ses questions : recharger en revenant sur l'onglet changerait la question en cours.
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  })
}

/** Corrige une réponse de l'essai, sans rien enregistrer. */
export function useCheckTrialAnswer(subject: string, notion: string) {
  return useMutation({
    mutationFn: async (answer: PracticeAnswer) => {
      const { data, error, response } = await api.POST(
        '/api/v1/subjects/{subject}/trial/{notion}/checks',
        { params: { path: { subject, notion } }, body: answer },
      )
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}
