import { useId, type ReactNode } from 'react'
import { Link } from 'react-router'
import { TEXT_LINK } from './textLink'

/**
 * Une page de texte (ADR 0074) : des lignes courtes, un titre, et la date de sa dernière mise à jour, écrite dans la
 * page pour qu'on sache quelle version on a lue.
 */
export function LegalPage({
  title,
  updatedOn,
  children,
}: {
  title: string
  updatedOn: string
  children: ReactNode
}) {
  return (
    <article className="flex max-w-prose flex-col gap-8 pb-4 leading-relaxed text-ink-soft">
      <header className="flex flex-col gap-2">
        <h1 className="text-3xl leading-tight font-bold text-ink">{title}</h1>
        <p className="text-sm text-ink-muted">Dernière mise à jour : {updatedOn}.</p>
      </header>
      {children}
    </article>
  )
}

export function LegalSection({ title, children }: { title: string; children: ReactNode }) {
  const id = useId()
  return (
    <section aria-labelledby={id} className="flex flex-col gap-3">
      <h2 id={id} className="text-xl font-bold text-ink">
        {title}
      </h2>
      {children}
    </section>
  )
}

export function LegalList({ children }: { children: ReactNode }) {
  return (
    <ul className="flex list-disc flex-col gap-1.5 pl-5 marker:text-line-strong">{children}</ul>
  )
}

/**
 * Un tableau, dans une zone qui défile de côté s'il ne tient pas : la page, elle, ne défile jamais de côté, même à
 * 320 px. La zone se parcourt au clavier (elle prend le focus) et porte le nom du tableau.
 */
export function LegalTable({
  caption,
  head,
  rows,
}: {
  caption: string
  head: string[]
  rows: ReactNode[][]
}) {
  const id = useId()
  return (
    <div
      role="region"
      aria-labelledby={id}
      tabIndex={0}
      className="overflow-x-auto rounded-xl border border-line bg-surface"
    >
      <table className="w-full min-w-[34rem] border-collapse text-left text-sm">
        <caption id={id} className="px-3 pt-3 pb-2 text-left font-bold text-ink">
          {caption}
        </caption>
        <thead>
          <tr>
            {head.map((label) => (
              <th
                key={label}
                scope="col"
                className="border-b border-line-strong px-3 py-2 align-bottom font-bold text-ink"
              >
                {label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((cells, row) => (
            <tr key={row} className="border-b border-line last:border-b-0">
              {cells.map((cell, column) =>
                column === 0 ? (
                  <th
                    key={column}
                    scope="row"
                    className="px-3 py-2 align-top font-semibold text-ink"
                  >
                    {cell}
                  </th>
                ) : (
                  <td key={column} className="px-3 py-2 align-top">
                    {cell}
                  </td>
                ),
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

/** L'adresse de contact, en lien ; sans elle (`npm run dev`, sans nginx), la page le dit plutôt que d'en inventer une. */
export function ContactEmail({ email }: { email: string | undefined }) {
  if (!email) return <span>l’adresse de contact (non configurée sur cette installation)</span>
  return (
    <a href={`mailto:${email}`} className={`${TEXT_LINK} wrap-anywhere`}>
      {email}
    </a>
  )
}

/**
 * La phrase qui mène aux deux pages légales, là où l'on entre dans l'application (ADR 0074) : elle informe, sans case
 * à cocher.
 */
export function LegalLinks({ lead }: { lead: string }) {
  return (
    <p className="text-sm text-ink-muted">
      {lead}{' '}
      <Link to="/confidentialite" className={TEXT_LINK}>
        politique de confidentialité
      </Link>{' '}
      et{' '}
      <Link to="/mentions-legales" className={TEXT_LINK}>
        mentions légales
      </Link>
      .
    </p>
  )
}
