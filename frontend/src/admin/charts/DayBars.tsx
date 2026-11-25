import { useId } from 'react'
import { ScrollTable } from './ScrollTable'

export type DayPoint = { label: string; value: number }

/** La hauteur du dessin, en pixels : sa largeur suit celle de la page. */
const HEIGHT = 168
/** La place du repère du haut, au-dessus de la plus haute barre. */
const TOP = 24
const PLOT = HEIGHT - TOP - 1
/** Une barre occupe les sept dixièmes de sa colonne. */
const BAR_SHARE = 0.7
/** Une valeur non nulle se voit toujours, même à côté d'une valeur cent fois plus grande. */
const MIN_BAR = 3

const number = (value: number) => value.toLocaleString('fr-FR')

/**
 * Une barre par jour, dessinée en SVG sans bibliothèque (ADR 0076). Le dessin n'est jamais la seule source : il a un
 * titre et une description pour les lecteurs d'écran, et « Voir les chiffres » donne les mêmes valeurs dans un tableau.
 * Sans valeur, l'échelle vaut 1 : rien ne se divise par zéro, et aucune barre ne se dessine.
 */
export function DayBars({
  title,
  description,
  unit,
  points,
}: {
  title: string
  description: string
  /** L'en-tête de la colonne des valeurs dans le tableau. */
  unit: string
  points: DayPoint[]
}) {
  const id = useId()
  const max = Math.max(1, ...points.map((point) => point.value))
  const step = 100 / Math.max(1, points.length)
  const barHeight = (value: number) =>
    value === 0 ? 0 : Math.max(MIN_BAR, Math.round((value / max) * PLOT))

  return (
    <figure className="flex flex-col gap-2">
      <svg
        role="img"
        aria-labelledby={`${id}-title`}
        aria-describedby={`${id}-desc`}
        width="100%"
        height={HEIGHT}
        className="overflow-visible"
      >
        <title id={`${id}-title`}>{title}</title>
        <desc id={`${id}-desc`}>{description}</desc>
        {/* Le repère du haut porte la plus grande valeur, celui du bas vaut zéro. */}
        <line x1="0" x2="100%" y1={TOP} y2={TOP} className="stroke-line" strokeDasharray="4 4" />
        <text x="0" y={TOP - 8} className="fill-ink-muted text-xs font-bold">
          {number(max)}
        </text>
        {points.map((point, index) => (
          <rect
            key={point.label}
            x={`${index * step + (step * (1 - BAR_SHARE)) / 2}%`}
            y={HEIGHT - 1 - barHeight(point.value)}
            width={`${step * BAR_SHARE}%`}
            height={barHeight(point.value)}
            rx="2"
            className="fill-brand"
          >
            <title>{`${point.label} : ${number(point.value)}`}</title>
          </rect>
        ))}
        <line x1="0" x2="100%" y1={HEIGHT - 0.5} y2={HEIGHT - 0.5} className="stroke-line-strong" />
      </svg>
      {points.length > 0 && (
        <p aria-hidden="true" className="flex justify-between text-xs text-ink-muted">
          <span>{points[0].label}</span>
          <span>{points[points.length - 1].label}</span>
        </p>
      )}
      <details className="text-sm">
        <summary className="cursor-pointer font-semibold text-brand-strong">
          Voir les chiffres
        </summary>
        <div className="mt-2">
          <ScrollTable caption={title} head={['Jour', unit]}>
            {points.map((point) => (
              <tr key={point.label} className="border-b border-line last:border-b-0">
                <th scope="row" className="px-3 py-1.5 text-left font-medium whitespace-nowrap">
                  {point.label}
                </th>
                <td className="px-3 py-1.5 text-right tabular-nums">{number(point.value)}</td>
              </tr>
            ))}
          </ScrollTable>
        </div>
      </details>
    </figure>
  )
}
