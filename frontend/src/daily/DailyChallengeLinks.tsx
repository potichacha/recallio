import { CheckCircle, HourglassMedium, Lightning, PlayCircle } from '@phosphor-icons/react'
import { Link } from 'react-router'
import { useDailyChallenges, type DailyChallengeSummary } from '../api/dailyChallenges'
import { useSession } from '../auth/session'
import { EmptyState } from '../components/states/EmptyState'
import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { button } from '../components/ui/button'
import { dailyChallengePath } from './dailyChallengeShare'

/**
 * Le défi du jour sur la page Jouer (ADR 0067) : chaque sujet qui en a un, les sujets suivis d'abord, et où en est
 * l'utilisateur. Un visiteur est invité à se connecter : jouer demande un compte.
 */
export function DailyChallengeSection() {
  const session = useSession()
  const signedIn = session.status === 'authenticated'
  const challenges = useDailyChallenges(signedIn)

  return (
    <section aria-labelledby="daily-challenges-title" className="flex flex-col gap-3">
      <header className="flex flex-col gap-1">
        <h2 id="daily-challenges-title" className="text-xl font-semibold">
          Défi du jour
        </h2>
        <p className="text-ink-muted">
          Cinq questions par sujet, les mêmes pour tout le monde, et une seule tentative.
        </p>
      </header>
      {session.status === 'anonymous' && (
        <button
          type="button"
          onClick={() => session.login('/play')}
          className={`self-start ${button()}`}
        >
          Connecte-toi pour jouer
        </button>
      )}
      {signedIn && challenges.isError && (
        <ErrorState
          message="Impossible de charger les défis du jour."
          onRetry={() => void challenges.refetch()}
        />
      )}
      {signedIn && challenges.isPending && (
        <LoadingState label="Chargement des défis du jour…" shape="tiles" />
      )}
      {challenges.data?.length === 0 && (
        <EmptyState
          title="Pas encore de défi"
          text="Aucun sujet n’a encore assez de questions pour un défi du jour."
        />
      )}
      {challenges.data && challenges.data.length > 0 && (
        <ul className="grid gap-3 sm:grid-cols-2">
          {challenges.data.map((summary) => (
            <li key={summary.subjectSlug}>
              <Link
                to={dailyChallengePath(summary.subjectSlug)}
                className="flex h-full items-center justify-between gap-3 rounded-xl border border-line bg-surface p-4 shadow-sm hover:border-brand-line"
              >
                <span className="text-lg font-semibold">{summary.subjectTitle}</span>{' '}
                <ChallengeState summary={summary} />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

/**
 * L'entrée discrète du défi du jour sur l'accueil, pour le sujet qu'il met en avant : à jouer, en cours ou fait. Rien
 * tant que la liste charge, ou si le sujet n'a pas de défi : l'accueil garde une seule action (ADR 0058).
 */
export function DailyChallengeShortcut({ subject }: { subject: string }) {
  const challenges = useDailyChallenges()
  const summary = challenges.data?.find((candidate) => candidate.subjectSlug === subject)
  if (!summary) return null
  return (
    <Link
      to={dailyChallengePath(subject)}
      className="flex items-center gap-3 rounded-2xl bg-surface px-4 py-3.5 hover:bg-brand-soft"
    >
      <Lightning weight="duotone" aria-hidden className="size-7 shrink-0 text-brand" />
      <span className="flex flex-col">
        <span className="font-extrabold">Défi du jour · {summary.subjectTitle}</span>{' '}
        <span className="text-sm font-bold text-ink-muted">{stateLabel(summary)}</span>
      </span>
    </Link>
  )
}

/** Où en est le défi d'un sujet : une icône et un mot, jamais la couleur seule. */
function ChallengeState({ summary }: { summary: DailyChallengeSummary }) {
  const Icon =
    summary.state === 'DONE'
      ? CheckCircle
      : summary.state === 'IN_PROGRESS'
        ? HourglassMedium
        : PlayCircle
  return (
    <span
      className={`inline-flex shrink-0 items-center gap-1 text-sm font-bold ${summary.state === 'DONE' ? 'text-success' : 'text-brand-strong'}`}
    >
      <Icon weight="fill" aria-hidden className="size-5" />
      {stateLabel(summary)}
    </span>
  )
}

function stateLabel(summary: DailyChallengeSummary): string {
  if (summary.state === 'DONE') return `Fait : ${summary.score ?? 0}/${summary.questionCount}`
  if (summary.state === 'IN_PROGRESS') return 'En cours'
  return 'À jouer'
}
