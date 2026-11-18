import { useFunnel, type FunnelStep, type FunnelWeek } from '../api/funnel'
import { EmptyState } from '../components/states/EmptyState'
import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'

const STEP_LABELS: Record<FunnelStep, string> = {
  VISIT: 'Visites',
  TRIAL: 'Essais',
  ACCOUNT: 'Comptes créés',
  FIRST_REVIEW: 'Premières révisions',
}

const STEPS = Object.keys(STEP_LABELS) as FunnelStep[]

// Le lundi d'une semaine est un jour calendaire : formaté en UTC, il ne glisse pas d'un jour.
const DAY = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short', timeZone: 'UTC' })
const formatDay = (isoDate: string) => DAY.format(new Date(`${isoDate}T00:00:00Z`))

const CELL = 'px-3 py-2 text-right tabular-nums'

/**
 * Le parcours des visiteurs (ADR 0064), pour les admins : visite, essai, compte créé, première révision, semaine par
 * semaine, avec la part de l'étape précédente. Des comptes sans identifiant : un taux compare deux étapes de la même
 * semaine, pas les mêmes personnes.
 */
export function FunnelSection() {
  const funnel = useFunnel()

  return (
    <section aria-labelledby="funnel-title" className="flex flex-col gap-3">
      <h2 id="funnel-title" className="text-xl font-semibold">
        Parcours des visiteurs
      </h2>
      <p id="funnel-help" className="text-sm text-ink-muted">
        Chaque étape compte les visiteurs qui l’ont franchie dans la semaine (du lundi au dimanche,
        en UTC), sans savoir qui ils sont. Sous chaque nombre, la part de l’étape précédente.
      </p>
      <FunnelContent funnel={funnel} />
    </section>
  )
}

function FunnelContent({ funnel }: { funnel: ReturnType<typeof useFunnel> }) {
  if (funnel.isError) {
    return (
      <ErrorState
        message="Impossible de charger le parcours."
        onRetry={() => void funnel.refetch()}
      />
    )
  }
  if (!funnel.data) return <LoadingState label="Chargement du parcours…" shape="card" />

  const { weeks } = funnel.data
  if (weeks.every((week) => week.steps.every((step) => step.count === 0))) {
    return (
      <EmptyState
        title="Personne n’est encore passé par là."
        text="Les visites de la page publique, les essais, les comptes et les premières révisions s’afficheront ici, semaine par semaine."
      />
    )
  }
  return (
    <div className="overflow-x-auto rounded-2xl border border-line bg-surface shadow-sm">
      <table
        aria-labelledby="funnel-title"
        aria-describedby="funnel-help"
        className="w-full text-sm"
      >
        <thead className="border-b border-line text-ink-muted">
          <tr>
            <th scope="col" className="px-3 py-2 text-left font-medium">
              Semaine du
            </th>
            {STEPS.map((step) => (
              <th key={step} scope="col" className={`${CELL} font-medium`}>
                {STEP_LABELS[step]}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {weeks.map((week) => (
            <WeekRow key={week.weekStart} week={week} />
          ))}
        </tbody>
      </table>
    </div>
  )
}

function WeekRow({ week }: { week: FunnelWeek }) {
  return (
    <tr className="border-b border-line last:border-b-0">
      <th scope="row" className="px-3 py-2 text-left font-medium whitespace-nowrap">
        {formatDay(week.weekStart)}
      </th>
      {STEPS.map((name, index) => {
        const step = week.steps.find((candidate) => candidate.step === name)
        return (
          <td key={name} className={CELL}>
            <span className="block font-bold">{(step?.count ?? 0).toLocaleString('fr-FR')}</span>
            {/* La première étape n'a pas d'étape précédente ; un tiret quand personne ne l'a franchie. */}
            {index > 0 && (
              <>
                {' '}
                <span className="block text-xs text-ink-muted">
                  {typeof step?.percent === 'number' ? (
                    <>
                      <span className="sr-only">soit </span>
                      {step.percent} %
                    </>
                  ) : (
                    '–'
                  )}
                </span>
              </>
            )}
          </td>
        )
      })}
    </tr>
  )
}
