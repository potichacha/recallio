import createClient from 'openapi-fetch'
import type { paths } from './schema'

/**
 * Client HTTP typé, généré depuis la spec OpenAPI du backend (`npm run generate:api`).
 * Ne jamais écrire d'appel `fetch` à la main vers l'API : passer par ce client.
 */
export const api = createClient<paths>({
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? globalThis.location?.origin ?? '',
  // Résolu à chaque appel (et non à la création du client) : les tests peuvent remplacer fetch.
  fetch: (request) => globalThis.fetch(request),
})
