import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { Link } from 'react-router'
import { ApiError } from '../api/errors'
import { useRetention } from '../api/retention'
import { FunnelSection } from './FunnelSection'

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

/**
 * Piloter le produit, pour les admins : la rétention (ADR 0050), parmi les joueurs dont le premier jour actif est assez
 * ancien, la part revenue réviser 1, 7 et 30 jours plus tard ; puis le parcours des visiteurs (ADR 0064), de la visite
 * à la première révision. Sans eux, on améliorerait l'application à l'aveugle.
 */
export function RetentionPage() {
  const retention = useRetention()

  if (retention.error instanceof ApiError && retention.error.status === 403) {
    return <p role="alert">Cette page est réservée aux administrateurs.</p>
  }

  return (
    <div className="flex flex-col gap-8">
      <h1 className="text-2xl font-bold">Rétention et parcours</h1>
      <section aria-labelledby="retention-title" className="flex flex-col gap-3">
        <h2 id="retention-title" className="text-xl font-semibold">
          Rétention
        </h2>
        <RetentionContent retention={retention} />
      </section>
      <FunnelSection />
      <Link to="/profile" className="self-start text-sm text-brand hover:underline">
        Retour au profil
      </Link>
    </div>
  )
}

function RetentionContent({ retention }: { retention: ReturnType<typeof useRetention> }) {
  if (retention.isError) {
    return (
      <ErrorState
        message="Impossible de charger la rétention."
        onRetry={() => void retention.refetch()}
      />
    )
  }
  if (!retention.data) return <LoadingState label="Chargement de la rétention…" shape="tiles" />

  const { players, days } = retention.data
  return (
    <>
      <p className="text-ink-muted">
        {plural(players, 'joueur')} {players > 1 ? 'ont' : 'a'} révisé au moins une fois. Un joueur
        compte à J7 s’il a révisé sept jours après son premier jour, jour pour jour.
      </p>
      <dl className="grid grid-cols-3 gap-3">
        {days.map((day) => (
          <div key={day.day} className="flex flex-col gap-1 rounded-xl bg-surface p-4 shadow-sm">
            <dt className="text-sm font-medium text-ink-muted">J{day.day}</dt>
            <dd className="text-2xl font-bold">{day.eligible > 0 ? `${day.percent} %` : '–'}</dd>
            <dd className="text-xs text-ink-muted">
              {day.retained} sur {plural(day.eligible, 'joueur')}
            </dd>
          </div>
        ))}
      </dl>
    </>
  )
}
