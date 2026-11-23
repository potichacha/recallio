/**
 * La configuration que nginx met dans index.html au démarrage du conteneur : une seule image du front sert tous les
 * environnements (ADR 0069). Chaque valeur est une balise `<meta name="recallio-…">` dont le marqueur (`__…__`) est
 * remplacé par une variable du conteneur ; sans nginx (`npm run dev`), le marqueur reste.
 */

/** Le contenu de la balise `<meta name="…">` d'index.html, tel que la page l'a reçu ; `undefined` si elle manque. */
export function metaContent(name: string): string | undefined {
  return globalThis.document?.querySelector<HTMLMetaElement>(`meta[name="${name}"]`)?.content
}

/**
 * La valeur que nginx a mise à la place du marqueur ; `undefined` si la balise manque, est vide, ou garde son marqueur
 * parce que la page n'est pas passée par nginx.
 */
export function servedValue(content: string | undefined, marker: string): string | undefined {
  return content && content !== marker ? content : undefined
}
