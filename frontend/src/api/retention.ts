import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type Retention = components['schemas']['Retention']

/** La rétention des joueurs à J1, J7 et J30 (ADR 0050), pour les admins. */
export function useRetention() {
  return useQuery({
    queryKey: ['admin', 'retention'],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/admin/retention')
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}
