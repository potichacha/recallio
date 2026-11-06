import { Link } from 'react-router'
import { GalleryPage } from './GalleryPage'

/** Cours (ADR 0052) : le cours d'un sujet, notion par notion, avec sa maîtrise. */
export function CoursesPage() {
  return (
    <GalleryPage
      title="Cours"
      intro="Lis le cours d’un sujet, notion par notion, et suis ta maîtrise."
      offers={(subject) => subject.cardCount > 0}
      linkTo={(subject) => `/subjects/${encodeURIComponent(subject.slug)}/course`}
    >
      <Link
        to="/subjects"
        className="self-start text-sm font-medium text-brand-strong hover:underline"
      >
        Parcourir toutes les cartes, thème par thème
      </Link>
    </GalleryPage>
  )
}
