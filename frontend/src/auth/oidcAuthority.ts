import { servedValue } from '../servedConfig'

/** Le Keycloak local, celui des Dev Services comme du compose. */
export const LOCAL_OIDC_AUTHORITY = 'http://localhost:8180/realms/recallio'

/** La balise d'index.html qui porte l'adresse du fournisseur OIDC. */
export const OIDC_AUTHORITY_TAG = 'recallio-oidc-authority'

/** Le marqueur de cette balise, tant que nginx ne l'a pas remplacé. */
export const OIDC_AUTHORITY_MARKER = '__OIDC_AUTHORITY__'

/**
 * L'adresse du fournisseur OIDC (ADR 0069). L'image du front sert tous les environnements : nginx met l'adresse du
 * conteneur (`OIDC_AUTHORITY`) dans la balise `recallio-oidc-authority` d'index.html à son démarrage. Sans nginx
 * (`npm run dev`), la balise garde son marqueur : on prend l'adresse donnée au build (`VITE_OIDC_AUTHORITY`), puis le
 * Keycloak local.
 */
export function oidcAuthority(
  servedByNginx: string | undefined,
  givenAtBuild: string | undefined,
): string {
  return servedValue(servedByNginx, OIDC_AUTHORITY_MARKER) ?? givenAtBuild ?? LOCAL_OIDC_AUTHORITY
}
