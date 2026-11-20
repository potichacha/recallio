import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type InterviewCountdown = components['schemas']['InterviewCountdown']
export type InterviewStatus = components['schemas']['InterviewStatus']
export type InterviewNotion = components['schemas']['InterviewNotion']
export type UpcomingInterviewWeek = components['schemas']['UpcomingInterviewWeek']

/** Clé partagée : fixer, changer ou retirer une date, ou changer de sujets suivis, l'invalide. */
export const INTERVIEWS_KEY = ['interviews']

/** Les comptes à rebours des sujets suivis qui ont une date d'entretien (ADR 0066). */
export function useInterviews(enabled = true) {
  return useQuery({
    queryKey: INTERVIEWS_KEY,
    enabled,
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/interviews')
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Fixe ou change la date de l'entretien d'un sujet suivi : le plan repart d'aujourd'hui. */
export function usePlanInterview() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ subject, date }: { subject: string; date: string }) => {
      const { data, error, response } = await api.PUT('/api/v1/me/interviews/{subject}', {
        params: { path: { subject } },
        body: { date },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: INTERVIEWS_KEY }),
  })
}

/** Retire la date de l'entretien d'un sujet. */
export function useCancelInterview() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (subject: string) => {
      const { error, response } = await api.DELETE('/api/v1/me/interviews/{subject}', {
        params: { path: { subject } },
      })
      if (!response.ok) throw ApiError.from(response, error)
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: INTERVIEWS_KEY }),
  })
}
