import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type Group = components['schemas']['Group']
export type GroupSummary = components['schemas']['GroupSummary']
export type GroupMember = components['schemas']['GroupMember']
export type GroupSettings = components['schemas']['GroupSettings']
export type LeaderboardVisibility = components['schemas']['LeaderboardVisibility']
export type Leaderboard = components['schemas']['Leaderboard']
export type LeaderboardEntry = components['schemas']['LeaderboardEntry']

const GROUPS_KEY = ['groups']
const leaderboardKey = (groupId: string) => [...GROUPS_KEY, groupId, 'leaderboard']

/** Les groupes dont l'utilisateur connecté est membre. */
export function useMyGroups() {
  return useQuery({
    queryKey: GROUPS_KEY,
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/groups')
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

export function useGroup(groupId: string) {
  return useQuery({
    queryKey: [...GROUPS_KEY, groupId],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/groups/{groupId}', {
        params: { path: { groupId } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

export function useCreateGroup() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (name: string) => {
      const { data, error, response } = await api.POST('/api/v1/groups', { body: { name } })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: async (group) => {
      queryClient.setQueryData([...GROUPS_KEY, group.id], group)
      await queryClient.invalidateQueries({ queryKey: GROUPS_KEY, exact: true })
    },
  })
}

/** Le créateur remplace les réglages du groupe ; la réponse est le groupe à jour. */
export function useUpdateGroupSettings(groupId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (settings: GroupSettings) => {
      const { data, error, response } = await api.PUT('/api/v1/groups/{groupId}/settings', {
        params: { path: { groupId } },
        body: settings,
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: async (group) => {
      queryClient.setQueryData([...GROUPS_KEY, group.id], group)
      // L'objectif et la visibilité changent le classement.
      await queryClient.invalidateQueries({ queryKey: leaderboardKey(group.id) })
    },
  })
}

/** Le classement de la semaine en cours ; sans lignes si le créateur l'a désactivé. */
export function useLeaderboard(groupId: string) {
  return useQuery({
    queryKey: leaderboardKey(groupId),
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/groups/{groupId}/leaderboard', {
        params: { path: { groupId } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/**
 * Les actions sans corps de réponse (204) : quitter, retirer un membre, supprimer le groupe.
 *
 * @param refreshGroup recharger aussi le groupe lui-même. Pas après l'avoir quitté ou supprimé : il répondrait 404, et la
 *     page afficherait « Groupe introuvable » au lieu de revenir à la liste.
 */
function useGroupAction<T>(
  call: (variables: T) => Promise<{ error?: unknown; response: Response }>,
  { refreshGroup }: { refreshGroup: boolean },
) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (variables: T) => {
      const { error, response } = await call(variables)
      if (!response.ok) throw ApiError.from(response, error)
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: GROUPS_KEY, exact: !refreshGroup }),
  })
}

export function useLeaveGroup() {
  return useGroupAction(
    (groupId: string) =>
      api.DELETE('/api/v1/groups/{groupId}/members/me', { params: { path: { groupId } } }),
    { refreshGroup: false },
  )
}

export function useRemoveMember() {
  return useGroupAction(
    ({ groupId, userId }: { groupId: string; userId: string }) =>
      api.DELETE('/api/v1/groups/{groupId}/members/{userId}', {
        params: { path: { groupId, userId } },
      }),
    { refreshGroup: true },
  )
}

export function useDeleteGroup() {
  return useGroupAction(
    (groupId: string) => api.DELETE('/api/v1/groups/{groupId}', { params: { path: { groupId } } }),
    { refreshGroup: false },
  )
}

export type GroupInvitation = components['schemas']['GroupInvitation']
export type Invitation = components['schemas']['Invitation']

const invitationKey = (groupId: string) => [...GROUPS_KEY, groupId, 'invitation']

/** L'adresse à partager : la page « rejoindre » de la PWA, qui porte le jeton. */
export function invitationUrl(token: string) {
  return `${window.location.origin}/join/${token}`
}

/** Le lien actif d'un groupe, ou `null` s'il n'en a pas. Réservé au créateur. */
export function useGroupInvitation(groupId: string) {
  return useQuery({
    queryKey: invitationKey(groupId),
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/groups/{groupId}/invitation', {
        params: { path: { groupId } },
      })
      if (response.status === 404) return null
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Génère un nouveau lien ; le précédent cesse de fonctionner. */
export function useRenewInvitation(groupId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      const { data, error, response } = await api.POST('/api/v1/groups/{groupId}/invitation', {
        params: { path: { groupId } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: (invitation) => queryClient.setQueryData(invitationKey(groupId), invitation),
  })
}

export function useRevokeInvitation(groupId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      const { error, response } = await api.DELETE('/api/v1/groups/{groupId}/invitation', {
        params: { path: { groupId } },
      })
      if (!response.ok) throw ApiError.from(response, error)
    },
    onSuccess: () => queryClient.setQueryData(invitationKey(groupId), null),
  })
}

/** Le groupe auquel invite ce lien. Un lien invalide ne le deviendra pas en réessayant : pas de nouvelle tentative. */
export function useInvitation(token: string) {
  return useQuery({
    queryKey: ['invitations', token],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/invitations/{token}', {
        params: { path: { token } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    retry: (failureCount, error) =>
      failureCount < 3 && !(error instanceof ApiError && error.status < 500),
  })
}

export function useAcceptInvitation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (token: string) => {
      const { data, error, response } = await api.POST('/api/v1/invitations/{token}/accept', {
        params: { path: { token } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    onSuccess: async (group) => {
      queryClient.setQueryData([...GROUPS_KEY, group.id], group)
      await queryClient.invalidateQueries({ queryKey: GROUPS_KEY, exact: true })
    },
  })
}
