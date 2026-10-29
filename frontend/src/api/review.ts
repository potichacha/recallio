import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import type { CardType } from './content'
import { ApiError } from './errors'
import type { components } from './schema'

export type ReviewQueue = components['schemas']['ReviewQueue']
export type QueuedCard = components['schemas']['QueuedCard']
export type Rating = components['schemas']['Rating']
export type ReviewResult = components['schemas']['ReviewResult']
export type RecordReviewRequest = components['schemas']['RecordReviewRequest']

export type ReviewFilter = { theme?: string; type?: CardType }

const QUEUE_KEY = ['review-queue']
export const QUEUE_SIZE = 20

/** Les cartes à réviser maintenant : d'abord les dues, puis des nouvelles dans la limite du jour. */
export function useReviewQueue(filter: ReviewFilter) {
  return useQuery({
    queryKey: [...QUEUE_KEY, filter],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/review-queue', {
        params: { query: { ...filter, size: QUEUE_SIZE } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    // La file est calculée à un instant donné : elle ne doit pas changer pendant qu'on la parcourt.
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  })
}

/**
 * Enregistre une note. Le `requestId` est choisi par l'appelant, une fois par réponse : les nouvelles tentatives
 * (réseau mobile instable) renvoient le même identifiant, et le serveur ne compte la révision qu'une fois.
 */
export function useRecordReview() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (review: RecordReviewRequest) => {
      const { data, error, response } = await api.POST('/api/v1/reviews', { body: review })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    retry: (failureCount, error) =>
      failureCount < 3 && !(error instanceof ApiError && error.status < 500),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUEUE_KEY, refetchType: 'none' }),
  })
}
