import { UserManager, WebStorageStateStore } from 'oidc-client-ts'
import { OIDC_AUTHORITY_TAG, oidcAuthority } from './oidcAuthority'

const origin = globalThis.location?.origin ?? 'http://localhost:5173'

/** L'adresse que porte la balise d'index.html, telle que nginx l'a servie. */
function servedAuthority(): string | undefined {
  const tag = globalThis.document?.querySelector<HTMLMetaElement>(
    `meta[name="${OIDC_AUTHORITY_TAG}"]`,
  )
  return tag?.content
}

/**
 * L'action par laquelle le fournisseur supprime l'identité de l'utilisateur, à sa demande (ADR 0056). C'est celle de
 * Keycloak (`kc_action`) ; vide, pour un fournisseur qui n'en a pas : l'utilisateur est seulement déconnecté.
 */
export const deleteIdentityAction: string =
  import.meta.env.VITE_OIDC_DELETE_IDENTITY_ACTION ?? 'delete_account'

/**
 * Connexion OIDC standard : flux Authorization Code avec PKCE (activé par défaut dans oidc-client-ts), client public
 * sans secret. L'adresse du fournisseur est celle que nginx met dans index.html au démarrage du conteneur (ADR 0069) ;
 * en dev, VITE_OIDC_AUTHORITY, ou le Keycloak des Dev Services, sur le port 8180 comme celui du compose.
 */
export const userManager = new UserManager({
  authority: oidcAuthority(servedAuthority(), import.meta.env.VITE_OIDC_AUTHORITY),
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID ?? 'recallio-frontend',
  redirect_uri: `${origin}/auth/callback`,
  post_logout_redirect_uri: origin,
  scope: 'openid profile email',
  // Jetons en sessionStorage : ils disparaissent à la fermeture de l'onglet (pas de stockage durable de secrets).
  userStore: new WebStorageStateStore({ store: globalThis.sessionStorage }),
  automaticSilentRenew: true,
})
