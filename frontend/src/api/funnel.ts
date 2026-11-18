import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'
import type { components } from './schema'

export type Funnel = components['schemas']['Funnel']
export type FunnelWeek = components['schemas']['FunnelWeek']
export type FunnelStep = components['schemas']['FunnelStep']

/** Le parcours des visiteurs des huit dernières semaines (ADR 0064), pour les admins. */
export function useFunnel() {
  return useQuery({
    queryKey: ['admin', 'funnel'],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/admin/funnel')
      if (data === undefined) throw ApiError.from(response, error)
      return data
    },
  })
}

const VISIT_KEY = 'recallio.visit-counted'

/**
 * Annonce une visite de la page publique (ADR 0064), une fois par session de navigateur : un drapeau en
 * `sessionStorage`, posé avant l'envoi pour qu'un second rendu n'en envoie pas une autre. Sans stockage (navigation
 * privée stricte), la visite compte quand même, au risque d'être comptée de nouveau au prochain passage sur la page.
 * Rien ne doit gêner le visiteur : une erreur d'envoi est ignorée.
 */
export function recordVisitOncePerSession() {
  try {
    if (sessionStorage.getItem(VISIT_KEY) !== null) return
    sessionStorage.setItem(VISIT_KEY, 'true')
  } catch {
    // Stockage indisponible : la visite compte, sans pouvoir s'en souvenir.
  }
  api.POST('/api/v1/funnel/visits').catch(() => {
    // Une mesure perdue ne vaut pas un message d'erreur.
  })
}
