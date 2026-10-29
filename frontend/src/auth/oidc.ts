import { UserManager, WebStorageStateStore } from 'oidc-client-ts'

const origin = globalThis.location?.origin ?? 'http://localhost:5173'

/**
 * Connexion OIDC standard : flux Authorization Code avec PKCE (activé par défaut dans oidc-client-ts), client public
 * sans secret. Le fournisseur se configure par VITE_OIDC_AUTHORITY ; en local, c'est le Keycloak des Dev Services ou du
 * compose, tous deux sur le port 8180.
 */
export const userManager = new UserManager({
  authority: import.meta.env.VITE_OIDC_AUTHORITY ?? 'http://localhost:8180/realms/recallio',
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID ?? 'recallio-frontend',
  redirect_uri: `${origin}/auth/callback`,
  post_logout_redirect_uri: origin,
  scope: 'openid profile email',
  // Jetons en sessionStorage : ils disparaissent à la fermeture de l'onglet (pas de stockage durable de secrets).
  userStore: new WebStorageStateStore({ store: globalThis.sessionStorage }),
  automaticSilentRenew: true,
})
