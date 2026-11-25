import type { ReactNode } from 'react'

/** Une section de la page Bêta : son titre de niveau 2, qui la nomme pour les lecteurs d'écran, puis son contenu. */
export function Section({
  id,
  title,
  children,
}: {
  id: string
  title: string
  children: ReactNode
}) {
  return (
    <section aria-labelledby={id} className="flex flex-col gap-3">
      <h2 id={id} className="text-xl font-semibold">
        {title}
      </h2>
      {children}
    </section>
  )
}
