import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type Subject = components['schemas']['Subject']
export type SubjectCategory = components['schemas']['SubjectCategory']
export type Theme = components['schemas']['Theme']
export type Card = components['schemas']['Card']
export type CardType = components['schemas']['CardType']
export type CardPage = components['schemas']['CardPage']

export const CARD_PAGE_SIZE = 20

export function useSubjects() {
  return useQuery({
    queryKey: ['subjects'],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/subjects')
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

export function useSubject(slug: string) {
  return useQuery({
    queryKey: ['subjects', slug],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/subjects/{slug}', {
        params: { path: { slug } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

export function useSubjectThemes(slug: string) {
  return useQuery({
    queryKey: ['subjects', slug, 'themes'],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/subjects/{slug}/themes', {
        params: { path: { slug } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

export function useTheme(slug: string) {
  return useQuery({
    queryKey: ['themes', slug],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/themes/{slug}', {
        params: { path: { slug } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

export function useThemeCards(slug: string, page: number) {
  return useQuery({
    queryKey: ['themes', slug, 'cards', page],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/themes/{slug}/cards', {
        params: { path: { slug }, query: { page, size: CARD_PAGE_SIZE } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    // Garde la page affichée pendant le chargement de la suivante : pas de clignotement.
    placeholderData: keepPreviousData,
  })
}
