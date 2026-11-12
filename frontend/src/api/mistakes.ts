import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { PracticeAnswer, PracticeCorrection } from './practice'

export const MISTAKES_KEY = ['mistakes']

/**
 * Le carnet d'erreurs d'un sujet (ADR 0057) : les cartes ratées cette semaine et pas réussies depuis. Une séance garde
 * ses cartes (`session`) : les recharger en revenant sur l'onglet changerait la carte en cours.
 */
export function useMistakes(subject: string, { session = false, enabled = true } = {}) {
  return useQuery({
    queryKey: [...MISTAKES_KEY, subject],
    enabled,
    staleTime: session ? Infinity : 0,
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/mistakes/{subject}', {
        params: { path: { subject } },
      })
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

/** Corrige la réponse à une carte du carnet, sans l'enregistrer. */
export async function checkMistakeAnswer(
  subject: string,
  answer: PracticeAnswer,
): Promise<PracticeCorrection> {
  const { data, error, response } = await api.POST('/api/v1/me/mistakes/{subject}/checks', {
    params: { path: { subject } },
    body: answer,
  })
  if (data === undefined) throw ApiError.from(response, error)
  return data
}
