import { useMutation, useQuery } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type Practice = components['schemas']['Practice']
export type PracticeCard = components['schemas']['PracticeCard']
export type PracticeAnswer = components['schemas']['PracticeAnswer']
export type PracticeCorrection = components['schemas']['PracticeCorrection']

/** Les cartes d'une notion, pour s'entraîner sans rien enregistrer (ADR 0038). */
export function usePractice(subject: string, notion: string) {
  return useQuery({
    queryKey: ['practice', subject, notion],
    // Une séance garde ses cartes : recharger en revenant sur l'onglet changerait la carte en cours.
    staleTime: Infinity,
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/practice/{subject}/{notion}', {
        params: { path: { subject, notion } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Corrige la réponse à une carte de l'entraînement, sans l'enregistrer. */
export function useCheckPracticeAnswer(subject: string, notion: string) {
  return useMutation({
    mutationFn: async (answer: PracticeAnswer) => {
      const { data, error, response } = await api.POST(
        '/api/v1/me/practice/{subject}/{notion}/checks',
        { params: { path: { subject, notion } }, body: answer },
      )
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}
