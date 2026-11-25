export type BarRow = {
  key: string
  label: string
  /** Une précision sous le libellé : le domaine d'un chapitre, « à ce jour »... */
  hint?: string
  /** Ce qui donne sa longueur à la barre. */
  value: number
  /** La valeur, écrite : « 3 comptes · 42 % ». */
  text: string
}

const BAR_HEIGHT = 10

/**
 * Des barres horizontales, dessinées en SVG sans bibliothèque (ADR 0076) : une ligne par élément, son libellé, sa
 * valeur écrite, puis sa barre. La valeur est toujours écrite à côté : la barre ne fait que la redire, et les lecteurs
 * d'écran ne la lisent pas. La liste porte le titre du graphe. Avec un maximum nul, l'échelle vaut 1 : rien ne se
 * divise par zéro.
 */
export function HorizontalBars({
  title,
  rows,
  max,
}: {
  title: string
  rows: BarRow[]
  /** La valeur d'une barre pleine ; par défaut, la plus grande des lignes. */
  max?: number
}) {
  const scale = Math.max(1, max ?? Math.max(0, ...rows.map((row) => row.value)))
  return (
    <ul aria-label={title} className="flex flex-col gap-3">
      {rows.map((row) => (
        <li key={row.key} className="flex flex-col gap-1">
          <div className="flex items-baseline justify-between gap-3 text-sm">
            <span className="min-w-0 font-semibold wrap-anywhere">
              {row.label}
              {row.hint && (
                <>
                  {' '}
                  <span className="font-medium text-ink-muted">· {row.hint}</span>
                </>
              )}
            </span>
            <span className="shrink-0 text-ink-soft tabular-nums">{row.text}</span>
          </div>
          <svg aria-hidden="true" width="100%" height={BAR_HEIGHT} className="block">
            <rect
              width="100%"
              height={BAR_HEIGHT}
              rx={BAR_HEIGHT / 2}
              className="fill-surface-sunken"
            />
            {row.value > 0 && (
              <rect
                width={`${Math.min(100, (row.value / scale) * 100)}%`}
                height={BAR_HEIGHT}
                rx={BAR_HEIGHT / 2}
                className="fill-brand"
              />
            )}
          </svg>
        </li>
      ))}
    </ul>
  )
}
