import { GalleryPage } from './GalleryPage'

/** Le Tour (ADR 0037, 0052) : apprendre un langage page par page, en exécutant chaque exemple. */
export function ToursPage() {
  return (
    <GalleryPage
      title="Le Tour"
      intro="Apprends un langage pas à pas, en codant à chaque page."
      offers={(subject) => subject.hasTour}
      linkTo={(subject) => `/subjects/${encodeURIComponent(subject.slug)}/tour`}
    />
  )
}
