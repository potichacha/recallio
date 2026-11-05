import { Link } from 'react-router'
import { ApiError } from '../api/errors'
import { useRetention } from '../api/retention'

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

/**
 * La rétention (ADR 0050), pour les admins : parmi les joueurs dont le premier jour actif est assez ancien, la part
 * revenue réviser 1, 7 et 30 jours plus tard. Sans elle, on améliorerait l'application à l'aveugle.
 */
export function RetentionPage() {
  const retention = useRetention()

  if (retention.error instanceof ApiError && retention.error.status === 403) {
    return <p role="alert">Cette page est réservée aux administrateurs.</p>
  }
  if (retention.isError) return <p role="alert">Impossible de charger la rétention.</p>
  if (!retention.data) return <p className="text-ink-muted">Chargement…</p>

  const { players, days } = retention.data
  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">Rétention</h1>
        <p className="text-ink-muted">
          {plural(players, 'joueur')} {players > 1 ? 'ont' : 'a'} révisé au moins une fois. Un
          joueur compte à J7 s’il a révisé sept jours après son premier jour, jour pour jour.
        </p>
      </header>
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
      <Link to="/profile" className="self-start text-sm text-brand hover:underline">
        Retour au profil
      </Link>
    </section>
  )
}
