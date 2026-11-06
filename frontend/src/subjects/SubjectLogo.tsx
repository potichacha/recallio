import { createElement } from 'react'
import type { Subject } from '../api/content'
import { categoryIcon, subjectLogo } from './subjectLogos'

/**
 * Le logo d'un sujet, sur un fond clair dans les deux thèmes : les logos gardent leurs couleurs, et un logo sombre (Rust)
 * reste lisible en mode sombre. Sans logo, l'icône de sa catégorie, prise dans le design system (ADR 0049). Il
 * accompagne le nom du sujet : il est décoratif.
 */
export function SubjectLogo({ subject }: { subject: Pick<Subject, 'slug' | 'category'> }) {
  const logo = subjectLogo(subject.slug)
  if (logo) {
    return (
      <span className="flex size-12 shrink-0 items-center justify-center rounded-xl border border-line bg-white p-2">
        <img src={logo} alt="" className="size-full object-contain" />
      </span>
    )
  }
  return (
    <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-brand-soft">
      {/* Une icône prise dans une table fixe : createElement, plutôt qu'un composant tiré pendant le rendu. */}
      {createElement(categoryIcon(subject.category), {
        weight: 'duotone',
        'aria-hidden': true,
        className: 'size-7 text-brand',
      })}
    </span>
  )
}
