import { useQuery } from '@tanstack/react-query'
import { api } from './client'

export type ApiStatus = 'UP' | 'DOWN'

/** Interroge la santé du backend. Un 503 (DOWN) est une réponse valide, pas une erreur réseau. */
export async function fetchApiStatus(): Promise<ApiStatus> {
  const { data, error } = await api.GET('/api/v1/health')
  const status = data?.status ?? error?.status
  if (status === undefined) {
    throw new Error('Réponse de santé inattendue')
  }
  return status
}

export function useApiStatus() {
  return useQuery({ queryKey: ['health'], queryFn: fetchApiStatus, retry: false })
}
