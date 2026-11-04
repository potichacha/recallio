import { useMutation, useQuery } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type TourOutline = components['schemas']['TourOutline']
export type TourNotion = components['schemas']['TourNotion']
export type TourPage = components['schemas']['TourPage']
export type ProgramRun = components['schemas']['ProgramRun']
export type ProgramFile = components['schemas']['ExerciseFile']

/** Le sommaire du Tour d'un sujet (ADR 0037) : public, comme le catalogue. */
export function useTour(subject: string) {
  return useQuery({
    queryKey: ['tour', subject],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/subjects/{subject}/tour', {
        params: { path: { subject } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Les pages d'une notion du Tour. */
export function useTourNotion(subject: string, notion: string) {
  return useQuery({
    queryKey: ['tour', subject, notion],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/subjects/{subject}/tour/{notion}', {
        params: { path: { subject, notion } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Lance un programme du Tour dans le bac à sable : rien n'est enregistré, une exécution à la fois. */
export function useRunProgram() {
  return useMutation({
    mutationFn: async (files: ProgramFile[]) => {
      const { data, error, response } = await api.POST('/api/v1/playground-runs', {
        body: { files },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}
