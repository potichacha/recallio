import { Link } from 'react-router'
import type { DailyChallenge, DailyChallengeStats } from '../api/dailyChallenges'
import { Celebration } from '../components/Celebration'
import { Rico } from '../components/Rico'
import { ShareButton } from '../components/ShareButton'
import { ChallengeMarks } from './ChallengeMarks'
import { dailyChallengeShareCard } from './dailyChallengeShare'

const AVERAGE = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 })

const players = (count: number) => `${count} joueur${count > 1 ? 's' : ''}`

/**
 * Le résultat d'un défi du jour fini (ADR 0067) : le score, la suite des réussites et des erreurs, les chiffres du jour
 * et le partage. Revenir sur la page le montre de nouveau : il n'y a jamais de second essai.
 */
export function DailyChallengeResult({ challenge }: { challenge: DailyChallenge }) {
  const score = challenge.score ?? 0
  const total = challenge.questionCount
  const perfect = score === total
  return (
    <section aria-labelledby="daily-result-title" className="flex flex-col gap-5">
      <div className="relative flex items-center gap-4">
        {perfect && <Celebration />}
        <Rico pose={perfect ? 'cheering' : 'neutral'} decorative />
        <div className="flex flex-1 flex-col gap-1 rounded-2xl bg-brand p-5 text-on-brand shadow-press">
          <h2 id="daily-result-title" className="text-sm font-bold uppercase">
            {perfect ? 'Sans faute !' : 'Défi terminé'}
          </h2>
          <p className="text-4xl font-extrabold motion-safe:animate-pop">
            {score}/{total}
          </p>
        </div>
      </div>
      <ChallengeMarks challenge={challenge} />
      {challenge.stats && <DayFigures stats={challenge.stats} total={total} />}
      <div className="flex flex-col items-start gap-3">
        <ShareButton card={dailyChallengeShareCard(challenge, window.location.origin)} />
        <p className="text-ink-soft">Reviens demain pour le prochain défi.</p>
        <Link to="/play" className="text-sm font-medium text-brand-strong underline">
          Les défis des autres sujets
        </Link>
      </div>
    </section>
  )
}

/** Les chiffres du jour, agrégés, sans aucun nom : chaque score avec son nombre de joueurs, en barre et en texte. */
function DayFigures({ stats, total }: { stats: DailyChallengeStats; total: number }) {
  const most = Math.max(1, ...stats.distribution.map((bar) => bar.players))
  return (
    <section
      aria-labelledby="daily-figures-title"
      className="flex flex-col gap-3 rounded-2xl border border-line bg-surface p-5 shadow-sm"
    >
      <h2 id="daily-figures-title" className="text-lg font-semibold">
        Les chiffres du jour
      </h2>
      <p className="text-ink-soft">
        {players(stats.players)} {stats.players > 1 ? 'ont' : 'a'} fini ce défi · score moyen :{' '}
        {AVERAGE.format(stats.averageScore)} sur {total}
      </p>
      <ul aria-label="Répartition des scores" className="flex flex-col gap-1.5">
        {[...stats.distribution].reverse().map((bar) => (
          <li key={bar.score} className="flex items-center gap-3 text-sm">
            <span className="w-10 shrink-0 font-mono font-bold">
              {bar.score}/{total}
            </span>
            <span aria-hidden className="h-3 flex-1 overflow-hidden rounded-full bg-surface-sunken">
              <span
                className="block h-full rounded-full bg-brand"
                style={{ width: `${Math.round((bar.players / most) * 100)}%` }}
              />
            </span>
            <span className="w-24 shrink-0 text-right text-ink-muted">{players(bar.players)}</span>
          </li>
        ))}
      </ul>
    </section>
  )
}
