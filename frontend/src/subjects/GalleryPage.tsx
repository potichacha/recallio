import type { ReactNode } from 'react'
import { useCatalogSubjects, type Subject } from '../api/content'
import { SubjectGallery } from './SubjectGallery'
import { useIsAdmin } from './useIsAdmin'

/**
 * Une page qui s'ouvre sur les sujets (ADR 0052) : Cours, Tour ou Révisions. Un sujet s'ouvre s'il est publié (ou pour
 * un admin, qui relit les brouillons) et s'il a ce que la page propose ; les autres sont annoncés en préparation.
 */
export function GalleryPage({
  title,
  intro,
  offers,
  linkTo,
  children,
}: {
  title: string
  intro: string
  /** Vrai si le sujet a ce que la page propose : des cartes, un Tour... */
  offers: (subject: Subject) => boolean
  linkTo: (subject: Subject) => string
  children?: ReactNode
}) {
  const subjects = useCatalogSubjects()
  const admin = useIsAdmin()

  return (
    <section className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">{title}</h1>
        <p className="text-ink-muted">{intro}</p>
      </header>
      {subjects.isPending && <p>Chargement des sujets…</p>}
      {subjects.isError && <p role="alert">Impossible de charger les sujets.</p>}
      {subjects.isSuccess && (
        <SubjectGallery
          subjects={subjects.data}
          isOpen={(subject) => (subject.status === 'PUBLISHED' || admin) && offers(subject)}
          linkTo={linkTo}
        />
      )}
      {children}
    </section>
  )
}
