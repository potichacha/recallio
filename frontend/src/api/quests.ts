import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import { PROGRESS_KEY } from './progress'
import type { components } from './schema'

export type QuestBoard = components['schemas']['QuestBoard']
export type QuestProgress = components['schemas']['QuestProgress']
export type Quest = components['schemas']['Quest']
export type AchievementStatus = components['schemas']['AchievementStatus']
export type Achievement = components['schemas']['Achievement']

/** Sous la clé de la progression : une révision enregistrée les rafraîchit aussi (voir useRecordReview). */
const QUESTS_KEY = [...PROGRESS_KEY, 'quests']
const ACHIEVEMENTS_KEY = [...PROGRESS_KEY, 'achievements']

/** Les quêtes du jour et de la semaine de l'utilisateur connecté (ADR 0040). */
export function useQuests() {
  return useQuery({
    queryKey: QUESTS_KEY,
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/quests')
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Tous les succès, débloqués ou non, dans l'ordre du catalogue (ADR 0040). */
export function useAchievements() {
  return useQuery({
    queryKey: ACHIEVEMENTS_KEY,
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/achievements')
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}
