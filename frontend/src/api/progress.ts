import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type Progress = components['schemas']['Progress']
export type SubjectXp = components['schemas']['SubjectXp']

/** Clé partagée : une révision enregistrée invalide la progression (voir useRecordReview). */
export const PROGRESS_KEY = ['progress']

/** XP (totale et par sujet suivi, ADR 0035), streak et objectif du jour de l'utilisateur connecté. */
export function useProgress(enabled = true) {
  return useQuery({
    queryKey: PROGRESS_KEY,
    enabled,
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/progress')
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}
