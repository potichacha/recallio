import { metaContent, servedValue } from '../servedConfig'

/** Les balises d'index.html qui portent l'éditeur et son adresse de contact, et leurs marqueurs avant nginx. */
export const LEGAL_PUBLISHER_TAG = 'recallio-legal-publisher'
export const LEGAL_PUBLISHER_MARKER = '__LEGAL_PUBLISHER__'
export const LEGAL_CONTACT_EMAIL_TAG = 'recallio-legal-contact-email'
export const LEGAL_CONTACT_EMAIL_MARKER = '__LEGAL_CONTACT_EMAIL__'

/**
 * Qui édite le site, et comment le joindre (ADR 0074). Le dépôt est public : ces coordonnées n'y sont pas écrites.
 * nginx les met dans index.html au démarrage du conteneur (`LEGAL_PUBLISHER`, `LEGAL_CONTACT_EMAIL`), comme l'adresse
 * du fournisseur OIDC (ADR 0069).
 */
export type LegalConfig = {
  /** Le nom de l'éditeur ; absent, il a choisi de n'être connu que de l'hébergeur. */
  publisher?: string
  /** L'adresse de contact ; absente quand la page n'est pas passée par nginx (`npm run dev`). */
  contactEmail?: string
}

/** Lit les deux balises : une valeur vide, ou restée à son marqueur, est absente. */
export function legalConfig(): LegalConfig {
  return {
    publisher: servedValue(metaContent(LEGAL_PUBLISHER_TAG)?.trim(), LEGAL_PUBLISHER_MARKER),
    contactEmail: servedValue(
      metaContent(LEGAL_CONTACT_EMAIL_TAG)?.trim(),
      LEGAL_CONTACT_EMAIL_MARKER,
    ),
  }
}
