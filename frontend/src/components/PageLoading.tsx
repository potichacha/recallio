import { LoadingState } from './states/LoadingState'
/** Ce qu'affiche le layout le temps de télécharger le code d'une page, à l'ouverture de l'application. */
export function PageLoading() {
  return <LoadingState label="Chargement…" />
}
