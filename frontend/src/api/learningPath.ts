import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type LearningPath = components['schemas']['LearningPath']
export type PathDomain = components['schemas']['PathDomain']
export type PathNotion = components['schemas']['PathNotion']
export type NotionStatus = PathNotion['status']
export type MasteryLevel = components['schemas']['MasteryLevel']
export type NextMasteryLevel = components['schemas']['NextMasteryLevel']
export type MasteryCounts = components['schemas']['MasteryCounts']

/**
 * Le parcours d'un sujet (ADR 0032) : ses notions par domaine et par chapitre, chacune avec son état et sa maîtrise
 * (ADR 0034) pour l'utilisateur connecté.
 */
export function useLearningPath(subject: string) {
  return useQuery({
    queryKey: ['learning-path', subject],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/learning-paths/{subject}', {
        params: { path: { subject } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}
