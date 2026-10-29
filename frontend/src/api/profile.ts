import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type Profile = components['schemas']['Profile']
export type ProfileUpdate = components['schemas']['UpdateProfileRequest']

const PROFILE_KEY = ['me']

export function browserTimezone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone
}

/** Le profil de l'utilisateur connecté. Le premier appel le crée côté serveur, avec le fuseau du navigateur. */
export function useProfile(enabled = true) {
  return useQuery({
    queryKey: PROFILE_KEY,
    enabled,
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me', {
        params: { header: { 'X-Timezone': browserTimezone() } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

export function useUpdateProfile() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (update: ProfileUpdate) => {
      const { data, error, response } = await api.PUT('/api/v1/me', { body: update })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: (profile) => queryClient.setQueryData(PROFILE_KEY, profile),
  })
}
