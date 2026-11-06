import { keepPreviousData, queryOptions, useQueries, useQuery } from '@tanstack/react-query'
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

/**
 * Tous les sujets, ceux en préparation compris (ADR 0052) : les pages Cours, Tour et Révisions les annoncent, mais seul
 * un admin ouvre un brouillon.
 */
export function useCatalogSubjects() {
  return useQuery({
    queryKey: ['catalog-subjects'],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/subjects', {
        params: { query: { upcoming: true } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

export type NotionLesson = components['schemas']['NotionLesson']

/** La leçon d'une notion et ses voisines, pour la page Cours (ADR 0052). */
export function useNotionLesson(subject: string, notion: string) {
  return useQuery({
    queryKey: ['notion-lessons', subject, notion],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/subjects/{slug}/notions/{notion}', {
        params: { path: { slug: subject, notion } },
      })
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

function subjectThemesQuery(slug: string) {
  return queryOptions({
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

export function useSubjectThemes(slug: string) {
  return useQuery(subjectThemesQuery(slug))
}

/**
 * Les thèmes de plusieurs sujets, dans l'ordre des sujets demandés. Tant que `pending` est vrai, ceux d'un sujet pas
 * encore chargé manquent.
 */
export function useThemesOfSubjects(slugs: string[]) {
  return useQueries({
    queries: slugs.map((slug) => subjectThemesQuery(slug)),
    combine: (results) => ({
      themes: results.flatMap((result) => result.data ?? []),
      pending: results.some((result) => result.isPending),
    }),
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
