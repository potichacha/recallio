import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type MockExam = components['schemas']['MockExam']
export type MockExamResult = components['schemas']['MockExamResult']
export type MockExamSummary = components['schemas']['MockExamSummary']
export type ExamAnswer = components['schemas']['ExamAnswer']

const historyKey = (subject: string) => ['mock-exams', subject]

/** Les dernières copies rendues dans un sujet (ADR 0044). */
export function useMockExamHistory(subject: string) {
  return useQuery({
    queryKey: historyKey(subject),
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/mock-exams', {
        params: { query: { subject } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Un examen commencé, pour le reprendre après un rechargement de la page. */
export function useMockExam(id: string | null) {
  return useQuery({
    queryKey: ['mock-exam', id],
    enabled: id !== null,
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/mock-exams/{id}', {
        params: { path: { id: id ?? '' } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

export function useStartMockExam() {
  return useMutation({
    mutationFn: async (subject: string) => {
      const { data, error, response } = await api.POST('/api/v1/me/mock-exams', {
        body: { subject },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Rend la copie : la note et le corrigé ; l'historique du sujet se met à jour. */
export function useSubmitMockExam(subject: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, answers }: { id: string; answers: ExamAnswer[] }) => {
      const { data, error, response } = await api.POST('/api/v1/me/mock-exams/{id}/submission', {
        params: { path: { id } },
        body: { answers },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: historyKey(subject) }),
  })
}
