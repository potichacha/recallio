import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api } from './client'
import type { components } from './schema'

export type Theme = components['schemas']['Theme']
export type Card = components['schemas']['Card']
export type CardType = components['schemas']['CardType']
export type CardPage = components['schemas']['CardPage']

export const CARD_PAGE_SIZE = 20

/** Erreur d'API portant le code stable du Problem Details (RFC 9457) renvoyé par le backend. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

function toApiError(response: Response, error: unknown): ApiError {
  const problem = error as Partial<components['schemas']['ProblemDetail']> | undefined
  return new ApiError(
    response.status,
    problem?.code ?? 'unknown',
    problem?.detail ?? `Erreur ${response.status}`,
  )
}

export function useThemes() {
  return useQuery({
    queryKey: ['themes'],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/themes')
      if (data === undefined) throw toApiError(response, error)
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
      if (data === undefined) throw toApiError(response, error)
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
      if (data === undefined) throw toApiError(response, error)
      return data
    },
    // Garde la page affichée pendant le chargement de la suivante : pas de clignotement.
    placeholderData: keepPreviousData,
  })
}
