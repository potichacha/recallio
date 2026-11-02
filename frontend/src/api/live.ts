import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { api, currentAccessToken } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type LiveGame = components['schemas']['LiveGame']
export type LivePlayer = components['schemas']['LivePlayer']

const groupLiveKey = (groupId: string) => ['groups', groupId, 'live-games']

/** La partie ouverte du groupe, ou `null` s'il n'y en a pas. */
export function useGroupLiveGame(groupId: string) {
  return useQuery({
    queryKey: groupLiveKey(groupId),
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/groups/{groupId}/live-games', {
        params: { path: { groupId } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data[0] ?? null
    },
  })
}

/** Ouvre une partie dont l'utilisateur est l'hôte. */
export function useOpenLiveGame(groupId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (subject: string) => {
      const { data, error, response } = await api.POST('/api/v1/groups/{groupId}/live-games', {
        params: { path: { groupId } },
        body: { subject },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
    // Refusée parce qu'une partie existe déjà : la liste la montrera.
    onSettled: () => queryClient.invalidateQueries({ queryKey: groupLiveKey(groupId) }),
  })
}

export type LiveRound = components['schemas']['LiveRound']

/**
 * L'hôte démarre la partie. Rien à mettre en cache : le nouvel état arrive par le WebSocket, à tous les joueurs à la
 * fois.
 */
export function useStartLiveGame(gameId: string) {
  return useMutation({
    mutationFn: async () => {
      const { data, error, response } = await api.POST('/api/v1/live-games/{gameId}/start', {
        params: { path: { gameId } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** La réponse à la question en cours ; la correction arrive par le WebSocket, à la fin de la question. */
export function useAnswerLiveQuestion(gameId: string) {
  return useMutation({
    mutationFn: async ({
      question,
      selectedChoices,
    }: {
      question: number
      selectedChoices: number[]
    }) => {
      const { data, error, response } = await api.POST('/api/v1/live-games/{gameId}/answers', {
        params: { path: { gameId } },
        body: { question, selectedChoices },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Codes de fermeture envoyés par le serveur quand il refuse la connexion (ADR 0021). */
export const LIVE_CLOSE = { NOT_FOUND: 4404, CONFLICT: 4409 } as const

export type LiveConnection =
  | { status: 'connecting' }
  | { status: 'open'; game: LiveGame }
  | { status: 'closed'; code: number; reason: string }

type LiveEvent = { type: 'game'; game: LiveGame }

/**
 * Le WebSocket d'une partie. Un navigateur ne peut pas ajouter d'en-tête `Authorization` à un WebSocket : le jeton
 * voyage dans le sous-protocole, que le serveur replace en en-tête (mécanisme de Quarkus WebSockets Next).
 * Pour se reconnecter, le composant qui l'utilise est remonté (une `key` qui change).
 */
export function useLiveGameSocket(gameId: string): LiveConnection {
  const [connection, setConnection] = useState<LiveConnection>({ status: 'connecting' })

  useEffect(() => {
    let socket: WebSocket | undefined
    // Une connexion remplacée (démontage, reconnexion) ne doit plus toucher à l'état affiché.
    let current = true
    void currentAccessToken().then((token) => {
      if (!current) return
      const protocols = token
        ? [
            'bearer-token-carrier',
            encodeURIComponent(`quarkus-http-upgrade#Authorization#Bearer ${token}`),
          ]
        : []
      socket = new WebSocket(liveSocketUrl(gameId), protocols)
      socket.onmessage = (message: MessageEvent<string>) => {
        const event = JSON.parse(message.data) as LiveEvent
        if (current && event.type === 'game') setConnection({ status: 'open', game: event.game })
      }
      socket.onclose = (closed) => {
        if (current) setConnection({ status: 'closed', code: closed.code, reason: closed.reason })
      }
    })
    return () => {
      current = false
      socket?.close()
    }
  }, [gameId])

  return connection
}

function liveSocketUrl(gameId: string) {
  const origin = globalThis.location.origin.replace(/^http/, 'ws')
  return `${origin}/api/v1/live/${gameId}`
}
