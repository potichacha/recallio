import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type ReviewStats = components['schemas']['ReviewStats']
export type ThemeStats = components['schemas']['ThemeStats']
export type DayActivity = components['schemas']['DayActivity']

export const ACTIVITY_DAYS = 7

/** Cartes dues et progression par thème de l'utilisateur connecté. */
export function useReviewStats() {
  return useQuery({
    queryKey: ['review-stats'],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/review-stats')
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Cartes révisées chacun des derniers jours, du plus ancien à aujourd'hui. */
export function useRecentActivity(days = ACTIVITY_DAYS) {
  return useQuery({
    queryKey: ['activity', days],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/progress/activity', {
        params: { query: { days } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}
