import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { useInterviews } from '../api/interviews'
import { useProfile } from '../api/profile'
import type { SubjectXp } from '../api/progress'
import type { CardCounts, SubjectStats, ThemeStats } from '../api/stats'
import { MasteryBar } from '../components/MasteryBar'
import { ReadinessGauge } from '../components/ReadinessGauge'
import { masterySummary } from '../components/mastery'
import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { InterviewCountdownPanel } from './InterviewCountdownPanel'

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

/**
 * Progression par sujet suivi, dans l'ordre du catalogue : son niveau (ADR 0035), ses cartes, la maîtrise de ses
 * notions, son indice de préparation et le compte à rebours avant son entretien quand il a un référentiel (ADR 0034,
 * 0041, 0066), puis ses thèmes (dépliables). Avec un seul sujet, ses thèmes sont dépliés d'emblée.
 */
export function SubjectProgressList({
  subjects,
  levels,
}: {
  subjects: SubjectStats[]
  levels: SubjectXp[]
}) {
  // Seul un sujet qui a des notions essentielles a un plan avant un entretien (ADR 0066).
  const interviews = useInterviews(subjects.some((subject) => subject.readiness.essentials > 0))
  const profile = useProfile()

  /** Le compte à rebours d'un sujet, sous sa jauge ; rien pour un sujet sans notion essentielle. */
  const interviewOf = (subject: SubjectStats): ReactNode => {
    if (subject.readiness.essentials === 0) return null
    if (interviews.isPending)
      return <LoadingState label="Chargement de ton entretien…" shape="line" />
    if (!interviews.isSuccess) return null
    return (
      <InterviewCountdownPanel
        subject={subject.slug}
        title={subject.title}
        countdown={interviews.data.find((countdown) => countdown.subjectSlug === subject.slug)}
        track={profile.data?.tracks[subject.slug] ?? 'PREPARE'}
      />
    )
  }

  return (
    <section aria-labelledby="subjects-progress-title" className="flex flex-col gap-3">
      <h2 id="subjects-progress-title" className="text-lg font-black">
        Progression par sujet
      </h2>
      {interviews.isError && (
        <ErrorState
          message="Impossible de charger tes entretiens."
          onRetry={() => void interviews.refetch()}
        />
      )}
      <ul className="flex flex-col gap-3">
        {subjects.map((subject) => (
          <SubjectRow
            key={subject.slug}
            subject={subject}
            level={levels.find((level) => level.slug === subject.slug)}
            expanded={subjects.length === 1}
            interview={interviewOf(subject)}
          />
        ))}
      </ul>
    </section>
  )
}

function SubjectRow({
  subject,
  level,
  expanded,
  interview,
}: {
  subject: SubjectStats
  level: SubjectXp | undefined
  expanded: boolean
  /** Le compte à rebours avant l'entretien, sous la jauge (ADR 0066). */
  interview: ReactNode
}) {
  // Les thèmes commencés d'abord, puis les autres, chacun dans l'ordre du document.
  const started = subject.themes.filter((theme) => theme.counts.seen > 0)
  const notStarted = subject.themes.filter(
    (theme) => theme.counts.seen === 0 && theme.counts.total > 0,
  )

  return (
    <li className="flex flex-col gap-3 rounded-2xl bg-surface p-4">
      <div className="flex items-center justify-between gap-3">
        <span className="flex items-center gap-2">
          <Link
            to={`/subjects/${subject.slug}`}
            className="text-lg font-extrabold hover:text-brand"
          >
            {subject.title}
          </Link>
          {level && (
            <span className="rounded-full bg-warning-soft px-2 py-0.5 text-xs font-bold text-warning">
              Niveau {level.level}
            </span>
          )}
        </span>
        <DueLink
          counts={subject.counts}
          to={`/review?subject=${encodeURIComponent(subject.slug)}`}
        />
      </div>
      {level && <LevelProgress label={subject.title} level={level} />}
      <Progress label={subject.title} counts={subject.counts} />
      {subject.notions.total > 0 && (
        <div className="flex flex-col gap-1">
          <MasteryBar counts={subject.notions} label={`Notions ${subject.title}`} />
          <p className="text-xs text-ink-muted">
            {masterySummary(subject.notions)} ·{' '}
            <Link
              to={`/subjects/${subject.slug}/course`}
              className="font-bold text-brand-strong hover:underline"
            >
              Carte des compétences
            </Link>
          </p>
        </div>
      )}
      {subject.readiness.essentials > 0 && (
        <ReadinessGauge
          subject={subject.slug}
          title={subject.title}
          readiness={subject.readiness}
        />
      )}
      {interview}
      <details open={expanded} className="group">
        <summary className="cursor-pointer text-sm font-bold text-brand-strong">
          {plural(subject.themes.length, 'thème')}
          {started.length > 0 && ` · ${plural(started.length, 'commencé')}`}
        </summary>
        <ul aria-label={`Thèmes ${subject.title}`} className="mt-3 flex flex-col gap-2">
          {[...started, ...notStarted].map((theme) => (
            <ThemeRow key={theme.slug} theme={theme} />
          ))}
        </ul>
      </details>
    </li>
  )
}

function ThemeRow({ theme }: { theme: ThemeStats }) {
  return (
    <li className="flex flex-col gap-2 rounded-lg border border-line p-3">
      <div className="flex items-center justify-between gap-3">
        <Link to={`/themes/${theme.slug}`} className="font-bold hover:text-brand">
          {theme.title}
        </Link>
        <DueLink counts={theme.counts} to={`/review?theme=${encodeURIComponent(theme.slug)}`} />
      </div>
      <Progress label={theme.title} counts={theme.counts} />
    </li>
  )
}

function DueLink({ counts, to }: { counts: CardCounts; to: string }) {
  if (counts.dueNow === 0) return null
  return (
    <Link
      to={to}
      className="shrink-0 rounded-lg bg-brand-soft px-2.5 py-1 text-sm font-bold text-brand-strong hover:bg-brand-soft"
    >
      {counts.dueNow} à revoir
    </Link>
  )
}

/** L'XP gagnée dans le niveau en cours, sur celle qu'il demande : les seuils viennent du serveur (ADR 0035). */
function LevelProgress({ label, level }: { label: string; level: SubjectXp }) {
  const gained = level.xp - level.levelXp
  const needed = level.nextLevelXp - level.levelXp
  return (
    <div className="flex items-center gap-3">
      <progress
        value={gained}
        max={needed}
        aria-label={`${label} : ${gained} / ${needed} XP vers le niveau ${level.level + 1}`}
        className="h-1.5 w-full overflow-hidden rounded-full [&::-moz-progress-bar]:bg-sun [&::-webkit-progress-bar]:bg-surface-sunken [&::-webkit-progress-value]:bg-sun"
      />
      <span className="shrink-0 text-xs text-ink-muted">
        {gained} / {needed} XP
      </span>
    </div>
  )
}

function Progress({ label, counts }: { label: string; counts: CardCounts }) {
  const { total, seen, mastered } = counts
  return (
    <div className="flex flex-col gap-1">
      <progress
        value={seen}
        max={total}
        aria-label={`${label} : ${seen} cartes vues sur ${total}`}
        className="h-1.5 w-full overflow-hidden rounded-full [&::-moz-progress-bar]:bg-brand [&::-webkit-progress-bar]:bg-surface-sunken [&::-webkit-progress-value]:bg-brand"
      />
      <p className="text-xs text-ink-muted">
        {seen} / {total} vues · {plural(mastered, 'maîtrisée')}
      </p>
    </div>
  )
}
