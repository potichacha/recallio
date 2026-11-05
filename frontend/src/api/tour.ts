import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type TourOutline = components['schemas']['TourOutline']
export type TourNotion = components['schemas']['TourNotion']
export type TourPage = components['schemas']['TourPage']
export type ProgramRun = components['schemas']['ProgramRun']
export type ProgramFile = components['schemas']['ExerciseFile']
export type TourQuestion = components['schemas']['TourQuestion']
export type TourAnswer = components['schemas']['TourAnswer']
export type TourCheck = components['schemas']['TourCheck']
export type TourLastPage = components['schemas']['TourStep']
export type TourTask = NonNullable<TourPage['task']>
export type TourTaskCheck = components['schemas']['TourTaskCheck']
export type TourExercise = components['schemas']['TourExercise']

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

/** Les notions du Tour d'un sujet que l'utilisateur a validées ; seulement pour un utilisateur connecté. */
export function useTourProgress(subject: string, enabled: boolean) {
  return useQuery({
    queryKey: ['tour-progress', subject],
    enabled,
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/tour/{subject}', {
        params: { path: { subject } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Note la page ouverte, que « Reprendre le Tour » rouvrira. Un échec ne s'affiche pas : ce n'est qu'un marque-page. */
export function useSaveTourLastPage(subject: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (lastPage: TourLastPage) => {
      const { error, response } = await api.PUT('/api/v1/me/tour/{subject}/last-page', {
        params: { path: { subject } },
        body: lastPage,
      })
      if (!response.ok) throw ApiError.from(response, error)
    },
    // La progression du sujet seulement : les questions de la notion ouverte n'ont pas changé.
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['tour-progress', subject], exact: true }),
  })
}

/** Juge l'« À toi » d'une page : le serveur exécute le programme et compare sa sortie à celle attendue. */
export function useCheckTourTask(subject: string, notion: string) {
  return useMutation({
    mutationFn: async (task: { page: number; files: ProgramFile[] }) => {
      const { data, error, response } = await api.POST('/api/v1/me/tour/{subject}/{notion}/tasks', {
        params: { path: { subject, notion } },
        body: task,
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Les questions qui valident une notion, et si l'utilisateur l'a déjà validée. */
export function useTourCheckpoint(subject: string, notion: string) {
  return useQuery({
    queryKey: ['tour-progress', subject, notion],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/tour/{subject}/{notion}', {
        params: { path: { subject, notion } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Corrige les réponses aux questions d'une notion ; toutes justes, la progression du sujet change. */
export function useCheckTourNotion(subject: string, notion: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (answers: TourAnswer[]) => {
      const { data, error, response } = await api.POST(
        '/api/v1/me/tour/{subject}/{notion}/checks',
        {
          params: { path: { subject, notion } },
          body: { answers },
        },
      )
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: async (check) => {
      if (check.validated) {
        await queryClient.invalidateQueries({ queryKey: ['tour-progress', subject] })
      }
    },
  })
}
