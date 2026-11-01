import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type Group = components['schemas']['Group']
export type GroupSummary = components['schemas']['GroupSummary']
export type GroupMember = components['schemas']['GroupMember']

const GROUPS_KEY = ['groups']

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
