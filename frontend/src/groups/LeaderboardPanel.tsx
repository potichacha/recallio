import { useLeaderboard, type Leaderboard, type LeaderboardEntry } from '../api/groups'

// Les dates de la semaine sont des jours calendaires : formatées en UTC, elles ne glissent pas d'un jour.
const DAY = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', timeZone: 'UTC' })
const formatDay = (isoDate: string) => DAY.format(new Date(`${isoDate}T00:00:00Z`))

/** Le classement de la semaine : la régularité d'abord (CLAUDE.md, section 2.4), puis l'XP (ADR 0035). */
export function LeaderboardPanel({ groupId }: { groupId: string }) {
  const leaderboard = useLeaderboard(groupId)

  return (
    <section
      aria-labelledby="leaderboard-title"
      className="flex flex-col gap-3 rounded-xl bg-surface p-4 shadow-sm"
    >
      <h2 id="leaderboard-title" className="text-lg font-semibold">
        Classement de la semaine
      </h2>
      {leaderboard.isPending && <p>Chargement du classement…</p>}
      {leaderboard.isError && <p role="alert">Impossible de charger le classement.</p>}
      {leaderboard.data && <LeaderboardContent leaderboard={leaderboard.data} />}
    </section>
  )
}

function LeaderboardContent({ leaderboard }: { leaderboard: Leaderboard }) {
  if (leaderboard.visibility === 'HIDDEN') {
    return <p className="text-ink-muted">Le créateur a désactivé le classement de ce groupe.</p>
  }
  return (
    <>
      <p className="text-sm text-ink-muted">
        Du {formatDay(leaderboard.weekStart)} au {formatDay(leaderboard.weekEnd)}. Un jour est
        régulier à partir de {leaderboard.dailyGoal} cartes révisées.
      </p>
      <ol className="flex flex-col gap-2">
        {leaderboard.entries.map((entry, index) => (
          <LeaderboardRow key={index} entry={entry} />
        ))}
      </ol>
    </>
  )
}

function LeaderboardRow({ entry }: { entry: LeaderboardEntry }) {
  const days = entry.regularDays
  return (
    <li
      className={`flex items-center justify-between gap-3 rounded-lg px-3 py-2 ${
        entry.me ? 'bg-brand-soft font-medium' : 'bg-surface-muted'
      }`}
    >
      <span className="flex items-center gap-3">
        <span className="w-6 text-right text-ink-muted">{entry.rank}</span>
        <span>
          {entry.displayName ?? 'Membre anonyme'}
          {entry.me && <span className="text-ink-muted"> (vous)</span>}
        </span>
      </span>
      <span className="text-sm text-ink-muted">
        {days} jour{days > 1 ? 's' : ''} régulier{days > 1 ? 's' : ''} · {entry.xp} XP
      </span>
    </li>
  )
}
