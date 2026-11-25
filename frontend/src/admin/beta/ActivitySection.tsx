import { useState } from 'react'
import type { BetaDay } from '../../api/beta'
import { Chip, Choices } from '../../components/ChoiceChips'
import { EmptyState } from '../../components/states/EmptyState'
import { DayBars } from '../charts/DayBars'
import { formatDay, formatNumber } from './format'
import { Section } from './Section'

type Series = Exclude<keyof BetaDay, 'day'>

/** Les séries du graphe, dans l'ordre des pastilles. `unit` est l'en-tête de la colonne dans le tableau des chiffres. */
const SERIES: { key: Series; label: string; unit: string; note: string }[] = [
  {
    key: 'reviewers',
    label: 'Comptes qui ont révisé',
    unit: 'Comptes',
    note: 'Les comptes qui ont révisé au moins une carte, au jour de chacun dans son fuseau.',
  },
  {
    key: 'reviews',
    label: 'Révisions',
    unit: 'Révisions',
    note: 'Les cartes révisées, au jour de chacun dans son fuseau.',
  },
  {
    key: 'goalsReached',
    label: 'Objectifs atteints',
    unit: 'Comptes',
    note: 'Les comptes qui ont atteint leur objectif du jour, au jour de chacun dans son fuseau.',
  },
  {
    key: 'accountsCreated',
    label: 'Comptes créés',
    unit: 'Comptes',
    note: 'Les comptes créés, au jour UTC. Un compte supprimé depuis n’est plus compté.',
  },
  {
    key: 'tourValidations',
    label: 'Validations du Tour',
    unit: 'Notions',
    note: 'Les notions validées dans le Tour, au jour UTC.',
  },
]

/** L'activité de chaque jour de la fenêtre : une série à la fois, au choix de l'admin. */
export function ActivitySection({ days }: { days: BetaDay[] }) {
  const [selected, setSelected] = useState<Series>('reviewers')
  const series = SERIES.find(({ key }) => key === selected) ?? SERIES[0]
  const points = days.map((day) => ({ label: formatDay(day.day), value: day[series.key] }))
  const quiet = points.every((point) => point.value === 0)

  return (
    <Section id="beta-activity" title="Activité par jour">
      <Choices legend="Série affichée">
        {SERIES.map(({ key, label }) => (
          <Chip
            key={key}
            name="beta-series"
            checked={key === selected}
            onChange={() => setSelected(key)}
          >
            {label}
          </Chip>
        ))}
      </Choices>
      <p className="text-sm text-ink-muted">{series.note}</p>
      {quiet ? (
        <EmptyState
          title="Rien sur ces trente jours."
          text={`« ${series.label} » : une barre par jour s’affichera ici dès le premier.`}
        />
      ) : (
        <div className="rounded-2xl border border-line bg-surface p-4 shadow-sm">
          <DayBars
            title={`${series.label}, par jour`}
            description={describe(points)}
            unit={series.unit}
            points={points}
          />
        </div>
      )}
    </Section>
  )
}

/** Ce qu'un lecteur d'écran entend à la place du dessin : la période, le total et le jour le plus fort. */
function describe(points: { label: string; value: number }[]): string {
  const total = points.reduce((sum, point) => sum + point.value, 0)
  const best = points.reduce((top, point) => (point.value > top.value ? point : top), points[0])
  return `Du ${points[0].label} au ${points[points.length - 1].label} : ${formatNumber(total)} au total, le plus haut le ${best.label} avec ${formatNumber(best.value)}. Le détail est dans « Voir les chiffres ».`
}
