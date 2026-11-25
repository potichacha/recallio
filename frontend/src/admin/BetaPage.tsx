import { Link } from 'react-router'
import { useBetaReport, type BetaReport } from '../api/beta'
import { ApiError } from '../api/errors'
import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { TEXT_LINK } from '../legal/textLink'
import { ActivitySection } from './beta/ActivitySection'
import { FailedCardsSection, ReportedCardsSection } from './beta/CardsSection'
import { FeaturesSection } from './beta/FeaturesSection'
import { formatDay, formatNumber } from './beta/format'
import { SubjectsSection } from './beta/SubjectsSection'
import { TourSection } from './beta/TourSection'

/**
 * La page « Bêta » des admins (ADR 0076) : ce que font les testeurs sur les trente derniers jours, en agrégé. Des
 * comptes et des parts, jamais une personne. La rétention et le parcours des visiteurs gardent leur page : celle-ci y
 * renvoie, elle ne les recopie pas.
 */
export function BetaPage() {
  const report = useBetaReport()

  if (report.error instanceof ApiError && report.error.status === 403) {
    return <p role="alert">Cette page est réservée aux administrateurs.</p>
  }

  return (
    <div className="flex flex-col gap-8">
      <header className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold">Bêta</h1>
        <p className="text-ink-muted">
          Ce que font les testeurs, en chiffres d’ensemble : des comptes et des parts, jamais une
          personne. Combien reviennent, et où l’on perd les visiteurs, c’est sur{' '}
          <Link to="/admin/retention" className={TEXT_LINK}>
            Rétention et parcours
          </Link>
          .
        </p>
      </header>
      {report.isError ? (
        <ErrorState
          message="Impossible de charger les chiffres de la bêta."
          onRetry={() => void report.refetch()}
        />
      ) : !report.data ? (
        <LoadingState label="Chargement des chiffres de la bêta…" shape="tiles" />
      ) : (
        <BetaContent report={report.data} />
      )}
      <Link to="/profile" className="self-start text-sm text-brand hover:underline">
        Retour au profil
      </Link>
    </div>
  )
}

function BetaContent({ report }: { report: BetaReport }) {
  const sum = (pick: (day: BetaReport['days'][number]) => number) =>
    report.days.reduce((total, day) => total + pick(day), 0)
  const figures = [
    { label: 'Comptes actifs', value: report.features.activeAccounts },
    { label: 'Révisions', value: sum((day) => day.reviews) },
    { label: 'Comptes créés', value: sum((day) => day.accountsCreated) },
    { label: 'Notions validées dans le Tour', value: sum((day) => day.tourValidations) },
  ]
  const quiet = figures.every(({ value }) => value === 0)

  return (
    <>
      <section aria-labelledby="beta-figures" className="flex flex-col gap-3">
        <h2 id="beta-figures" className="text-xl font-semibold">
          Les trente derniers jours
        </h2>
        <p className="text-sm text-ink-muted">
          Du {formatDay(report.from)} au {formatDay(report.to)}, en UTC. Un compte est actif s’il a
          révisé, validé une notion du Tour ou joué sur la période.
          {quiet &&
            ' Personne n’a encore rien fait sur cette période : les chiffres arriveront avec les premiers testeurs.'}
        </p>
        <dl className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {figures.map(({ label, value }) => (
            <div
              key={label}
              className="flex flex-col justify-between gap-1 rounded-xl bg-surface p-4 shadow-sm"
            >
              <dt className="text-sm font-medium text-ink-muted">{label}</dt>
              <dd className="text-2xl font-bold tabular-nums">{formatNumber(value)}</dd>
            </div>
          ))}
        </dl>
      </section>
      <ActivitySection days={report.days} />
      <SubjectsSection subjects={report.subjects} />
      <FailedCardsSection cards={report.failedCards} />
      <ReportedCardsSection cards={report.reportedCards} />
      <TourSection tour={report.tour} />
      <FeaturesSection features={report.features} />
    </>
  )
}
