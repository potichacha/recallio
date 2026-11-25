import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type BetaReport = components['schemas']['BetaReport']
export type BetaDay = components['schemas']['BetaDay']
export type BetaSubject = components['schemas']['BetaSubject']
export type BetaCard = components['schemas']['BetaCard']
export type BetaFailedCard = components['schemas']['BetaFailedCard']
export type BetaReportedCard = components['schemas']['BetaReportedCard']
export type BetaTour = components['schemas']['BetaTour']
export type BetaFeatures = components['schemas']['BetaFeatures']
export type UsageFeature = components['schemas']['UsageFeature']

/** Ce que font les testeurs sur les trente derniers jours, en agrégé (ADR 0076), pour les admins. */
export function useBetaReport() {
  return useQuery({
    queryKey: ['admin', 'beta'],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/admin/beta')
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}
