import { useState } from 'react'
import { Link } from 'react-router'
import type { Subject } from '../api/content'
import { SUBJECT_CATEGORIES } from '../components/subjectCategories'
import { SubjectLogo } from './SubjectLogo'

const TILE = 'flex h-full w-full items-center gap-3 rounded-xl border p-3 text-left'
// Dans une grille, une tuile ne rétrécit pas sous son contenu : un long titre (« Entretien comportemental ») la faisait
// déborder de l'écran d'un téléphone. Il se coupe donc plutôt que de pousser.
const TITLE = 'min-w-0 font-semibold wrap-anywhere'

/**
 * Les sujets d'une page (Cours, Tour ou Révisions, ADR 0052), rangés par catégorie, chacun avec son logo. Un sujet prêt
 * s'ouvre ; un sujet en préparation est grisé, et le toucher dit qu'il arrive. Une catégorie vide n'est pas affichée.
 */
export function SubjectGallery({
  subjects,
  isOpen,
  linkTo,
}: {
  subjects: Subject[]
  isOpen: (subject: Subject) => boolean
  linkTo: (subject: Subject) => string
}) {
  const [touched, setTouched] = useState<Subject>()

  return (
    <div className="flex flex-col gap-6">
      {/* Annonce, pour un lecteur d'écran, le sujet en préparation qu'on vient de toucher. */}
      <p role="status" className="sr-only">
        {touched && `${touched.title} : work in progress, ce sujet arrive bientôt.`}
      </p>
      {SUBJECT_CATEGORIES.map(({ category, label }) => {
        const inCategory = subjects.filter((subject) => subject.category === category)
        if (inCategory.length === 0) return null
        return (
          <section
            key={category}
            aria-labelledby={`gallery-${category}`}
            className="flex flex-col gap-3"
          >
            <h2 id={`gallery-${category}`} className="text-xl font-semibold">
              {label}
            </h2>
            <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3">
              {inCategory.map((subject) => (
                <li key={subject.slug} className="min-w-0">
                  {isOpen(subject) ? (
                    <Link
                      to={linkTo(subject)}
                      className={`${TILE} border-line bg-surface shadow-sm hover:border-brand-line`}
                    >
                      <SubjectLogo subject={subject} />
                      <span className={TITLE}>{subject.title}</span>
                    </Link>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setTouched(subject)}
                      className={`${TILE} border-dashed border-line-strong bg-surface-muted`}
                    >
                      <span className="opacity-60 grayscale">
                        <SubjectLogo subject={subject} />
                      </span>
                      <span className="flex min-w-0 flex-col">
                        <span className={`${TITLE} text-ink-soft`}>{subject.title}</span>{' '}
                        <span className="text-xs text-ink-muted">
                          {touched?.slug === subject.slug ? 'Work in progress' : 'En préparation'}
                        </span>
                      </span>
                    </button>
                  )}
                </li>
              ))}
            </ul>
          </section>
        )
      })}
    </div>
  )
}
