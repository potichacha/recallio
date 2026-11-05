import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type PublicProfile = components['schemas']['PublicProfile']
export type PublicSubject = components['schemas']['PublicSubject']
export type PublicProfileSettings = components['schemas']['PublicProfileSettings']
export type UpdatePublicProfileRequest = components['schemas']['UpdatePublicProfileRequest']

const SETTINGS_KEY = ['public-profile-settings']

/** L'adresse du profil public de l'utilisateur connecté, et s'il est visible (ADR 0043). */
export function usePublicProfileSettings() {
  return useQuery({
    queryKey: SETTINGS_KEY,
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/public-profile')
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

export function useUpdatePublicProfileSettings() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (request: UpdatePublicProfileRequest) => {
      const { data, error, response } = await api.PUT('/api/v1/me/public-profile', {
        body: request,
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: (settings) => {
      queryClient.setQueryData(SETTINGS_KEY, settings)
      return queryClient.invalidateQueries({ queryKey: ['public-profile', settings.handle] })
    },
  })
}

/** Un profil public, lisible sans compte ; un profil privé répond comme une adresse inconnue. */
export function usePublicProfile(handle: string) {
  return useQuery({
    queryKey: ['public-profile', handle],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/public-profiles/{handle}', {
        params: { path: { handle } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}
