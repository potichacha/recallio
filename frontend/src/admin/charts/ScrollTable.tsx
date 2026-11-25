import { useId, type ReactNode } from 'react'

/**
 * Un tableau de chiffres, dans une zone qui défile de côté s'il ne tient pas : la page, elle, ne défile jamais de côté,
 * même à 320 px. La zone se parcourt au clavier (elle prend le focus) et porte le nom du tableau. La première colonne
 * est alignée à gauche, les suivantes, des nombres, à droite.
 */
export function ScrollTable({
  caption,
  head,
  children,
}: {
  caption: string
  head: string[]
  children: ReactNode
}) {
  const id = useId()
  return (
    <div
      role="region"
      aria-labelledby={id}
      tabIndex={0}
      className="overflow-x-auto rounded-2xl border border-line bg-surface shadow-sm"
    >
      <table className="w-full text-sm">
        <caption id={id} className="sr-only">
          {caption}
        </caption>
        <thead className="border-b border-line text-ink-muted">
          <tr>
            {head.map((label, column) => (
              <th
                key={label}
                scope="col"
                className={`px-3 py-2 align-bottom font-medium ${column === 0 ? 'text-left' : 'text-right'}`}
              >
                {label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  )
}
