import createClient from 'openapi-fetch'
import type { paths } from './schema'

type AccessTokenProvider = () => Promise<string | undefined>

let accessTokenProvider: AccessTokenProvider = async () => undefined

/** Branché par la session OIDC : chaque appel d'API porte le jeton courant, s'il y en a un. */
export function setAccessTokenProvider(provider: AccessTokenProvider) {
  accessTokenProvider = provider
}

/**
 * Client HTTP typé, généré depuis la spec OpenAPI du backend (`npm run generate:api`).
 * Ne jamais écrire d'appel `fetch` à la main vers l'API : passer par ce client.
 */
export const api = createClient<paths>({
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? globalThis.location?.origin ?? '',
  // Résolu à chaque appel (et non à la création du client) : les tests peuvent remplacer fetch.
  fetch: (request) => globalThis.fetch(request),
})

api.use({
  async onRequest({ request }) {
    const token = await accessTokenProvider()
    if (token) {
      request.headers.set('Authorization', `Bearer ${token}`)
    }
    return request
  },
})
