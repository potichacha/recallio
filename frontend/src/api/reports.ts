import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type ReportReason = components['schemas']['ReportReason']
export type ReportResolution = components['schemas']['ReportResolution']
export type NewContentReport = components['schemas']['NewContentReport']
export type AdminContentReport = components['schemas']['AdminContentReport']
export type ReportStatus = components['schemas']['ReportStatus']

const ADMIN_REPORTS_KEY = ['admin-content-reports']

/** Signale une erreur dans une carte ou une page du Tour (ADR 0042). */
export function useReportContent() {
  return useMutation({
    mutationFn: async (report: NewContentReport) => {
      const { data, error, response } = await api.POST('/api/v1/content-reports', {
        body: report,
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Les signalements ouverts, ou traités, pour un admin (ADR 0042). */
export function useContentReports(status: ReportStatus, page: number) {
  return useQuery({
    queryKey: [...ADMIN_REPORTS_KEY, status, page],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/admin/content-reports', {
        params: { query: { status, page } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Traite un signalement : il quitte la liste des ouverts. */
export function useResolveReport() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, resolution }: { id: string; resolution: ReportResolution }) => {
      const { data, error, response } = await api.PUT(
        '/api/v1/admin/content-reports/{id}/resolution',
        { params: { path: { id } }, body: { resolution } },
      )
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ADMIN_REPORTS_KEY }),
  })
}
