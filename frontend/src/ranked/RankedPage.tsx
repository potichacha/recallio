import { EmptyState } from '../components/states/EmptyState'
import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { ApiError } from '../api/errors'
import {
  useFindDuel,
  useLeaveLive,
  useMyDuels,
  useQueueLive,
  useRankedOverview,
  type DuelSummary,
  type SubjectRank,
} from '../api/ranked'
import { RankIcon } from '../components/RankIcon'
import { rankLabel, TIER_LABELS } from '../components/rankLabels'
import { button } from '../components/ui/button'

const DATE = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long', timeZone: 'UTC' })

/** Le délai entre deux signaux dans la file du direct : le serveur oublie un joueur muet depuis 15 secondes. */
const LIVE_HEARTBEAT_MS = 3_000

const OUTCOMES: Record<NonNullable<DuelSummary['outcome']>, string> = {
  WIN: 'Gagné',
  LOSS: 'Perdu',
  DRAW: 'Nul',
}

/**
 * Le classé (ADR 0046) : un rang par sujet suivi, caché pendant le placement, des duels asynchrones ou en direct, et les
 * insignes des saisons passées. Les cotes ne quittent jamais le serveur.
 */
export function RankedPage() {
  const overview = useRankedOverview()
  const [live, setLive] = useState<string>()

  if (overview.isPending) return <LoadingState label="Chargement du classé…" />
  if (overview.isError) {
    return (
      <ErrorState
        message="Impossible de charger le classé."
        onRetry={() => void overview.refetch()}
      />
    )
  }
  const { season, seasonEndsOn, subjects, badges } = overview.data
  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">Classé</h1>
        <p className="text-sm text-ink-muted">
          Saison {season}, jusqu’au {DATE.format(new Date(`${seasonEndsOn}T00:00:00Z`))}. Cinq
          questions corrigées par le serveur, trente secondes chacune : le plus de bonnes réponses
          gagne, puis le plus rapide.
        </p>
      </header>
      {live ? (
        <LiveQueue subject={live} onCancel={() => setLive(undefined)} />
      ) : (
        <ul className="flex flex-col gap-3">
          {subjects.map((subject) => (
            <SubjectRankItem key={subject.subject} rank={subject} onLive={setLive} />
          ))}
        </ul>
      )}
      {subjects.length === 0 && (
        <EmptyState
          title="Aucun sujet suivi pour jouer en classé."
          text="Le classé se joue sur un sujet que tu suis : chacun y a son rang."
        >
          <Link to="/profile/subjects" className={button({ variant: 'secondary', size: 'sm' })}>
            Choisir mes sujets
          </Link>
        </EmptyState>
      )}
      {badges.length > 0 && (
        <section aria-labelledby="badges-title" className="flex flex-col gap-2">
          <h2 id="badges-title" className="text-lg font-semibold">
            Mes insignes de saison
          </h2>
          <ul className="flex flex-wrap gap-2">
            {badges.map((badge) => (
              <li
                key={`${badge.subject}-${badge.season}`}
                className="rounded-full bg-surface px-3 py-1 text-sm shadow-sm"
              >
                <RankIcon tier={badge.tier} /> {badge.title} · {badge.season} ·{' '}
                {TIER_LABELS[badge.tier]}
              </li>
            ))}
          </ul>
        </section>
      )}
      <RecentDuels />
    </section>
  )
}

function SubjectRankItem({
  rank,
  onLive,
}: {
  rank: SubjectRank
  onLive: (subject: string) => void
}) {
  const find = useFindDuel()
  const navigate = useNavigate()
  return (
    <li className="flex flex-col gap-3 rounded-xl bg-surface p-4 shadow-sm">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-lg font-semibold">{rank.title}</h2>
        <span className="font-medium">
          {rank.rank ? (
            <>
              <RankIcon tier={rank.rank.tier} /> {rankLabel(rank.rank)}
            </>
          ) : (
            `Placement : ${rank.seasonDuels} / ${rank.placementDuels}`
          )}
        </span>
      </div>
      <p className="text-sm text-ink-muted">
        {rank.wins} victoire{rank.wins > 1 ? 's' : ''}, {rank.losses} défaite
        {rank.losses > 1 ? 's' : ''}, {rank.draws} nul{rank.draws > 1 ? 's' : ''} cette saison
      </p>
      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          disabled={find.isPending}
          onClick={() =>
            find.mutate(rank.subject, { onSuccess: (duel) => navigate(`/ranked/duels/${duel.id}`) })
          }
          className={button()}
        >
          Duel
        </button>
        <button
          type="button"
          onClick={() => onLive(rank.subject)}
          className={button({ variant: 'secondary' })}
        >
          Duel en direct
        </button>
      </div>
      {find.isError && (
        <p role="alert" className="text-sm text-danger">
          {find.error instanceof ApiError ? find.error.message : 'Le duel n’a pas pu commencer.'}
        </p>
      )}
    </li>
  )
}

/** L'attente d'un adversaire en direct : un signal toutes les trois secondes, jusqu'au duel. */
function LiveQueue({ subject, onCancel }: { subject: string; onCancel: () => void }) {
  const queue = useQueueLive()
  const leave = useLeaveLive()
  const navigate = useNavigate()
  const { mutate } = queue

  useEffect(() => {
    const signal = () =>
      mutate(subject, {
        onSuccess: (status) => {
          if (status.duelId) navigate(`/ranked/duels/${status.duelId}`)
        },
      })
    signal()
    const timer = setInterval(signal, LIVE_HEARTBEAT_MS)
    return () => clearInterval(timer)
  }, [mutate, navigate, subject])

  const cancel = () => {
    leave.mutate()
    onCancel()
  }
  return (
    <div
      role="status"
      className="flex flex-col items-start gap-3 rounded-xl bg-surface p-5 shadow-sm"
    >
      <p className="font-medium">Recherche d’un adversaire en direct…</p>
      <p className="text-sm text-ink-muted">
        Reste sur cette page : dès qu’un autre joueur cherche aussi, le duel commence.
      </p>
      {queue.isError && (
        <p className="text-sm text-danger">
          {queue.error instanceof ApiError ? queue.error.message : 'La file ne répond pas.'}
        </p>
      )}
      <button
        type="button"
        onClick={cancel}
        className="rounded-lg px-3 py-1.5 font-medium text-ink-muted hover:bg-surface-muted"
      >
        Annuler
      </button>
    </div>
  )
}

function RecentDuels() {
  const duels = useMyDuels()
  if (!duels.data || duels.data.length === 0) return null
  return (
    <section aria-labelledby="recent-duels-title" className="flex flex-col gap-2">
      <h2 id="recent-duels-title" className="text-lg font-semibold">
        Mes derniers duels
      </h2>
      <ul className="flex flex-col gap-1 text-sm">
        {duels.data.map((duel) => (
          <li key={duel.id}>
            <Link to={`/ranked/duels/${duel.id}`} className="text-brand-strong hover:underline">
              {duel.subject} · {duel.mode === 'LIVE' ? 'direct' : 'asynchrone'}
              {duel.opponentName ? ` contre ${duel.opponentName}` : ''}
            </Link>{' '}
            : {status(duel)}
          </li>
        ))}
      </ul>
    </section>
  )
}

function status(duel: DuelSummary): string {
  if (duel.outcome) return OUTCOMES[duel.outcome]
  if (duel.status === 'EXPIRED') return 'expiré, sans adversaire'
  return duel.myTurnDone ? 'en attente de l’adversaire' : 'à jouer'
}
