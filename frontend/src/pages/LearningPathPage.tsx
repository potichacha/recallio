import { Trophy } from '@phosphor-icons/react'
import { Link, useParams } from 'react-router'
import { useSubject } from '../api/content'
import { ApiError } from '../api/errors'
import { useLearningPath, type PathDomain, type PathNotion } from '../api/learningPath'
import { MasteryBar } from '../components/MasteryBar'
import { ShareButton } from '../components/ShareButton'
import {
  MASTERY_LABELS,
  MASTERY_STYLES,
  masterySummary,
  nextLevelHint,
} from '../components/mastery'
import { TRACKS } from '../components/trackLabels'
import { practicePath } from '../practice/practicePath'

const LEVEL_LABELS: Record<PathNotion['level'], string> = {
  BEGINNER: 'débutant',
  INTERMEDIATE: 'intermédiaire',
  EXPERT: 'expert',
}

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

/**
 * Le cours d'un sujet (ADR 0052), qui est aussi sa carte des compétences : ses domaines, chacun avec la maîtrise de ses
 * notions (ADR 0034), puis ses chapitres et leurs notions, dont on ouvre la leçon. En parcours « apprendre », une notion
 * se débloque quand ses prérequis sont acquis (ADR 0032). Le domaine où l'on en est se déplie d'emblée.
 */
export function LearningPathPage() {
  const { slug = '' } = useParams()
  const subject = useSubject(slug)
  const path = useLearningPath(slug)

  if (path.error instanceof ApiError && path.error.status === 404) {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-bold">Sujet introuvable</h1>
        <Link to="/subjects" className="text-brand hover:underline">
          Retour aux sujets
        </Link>
      </section>
    )
  }

  const domains = path.data?.domains ?? []
  const current = currentDomain(domains)
  const track = TRACKS.find((candidate) => candidate.track === path.data?.track)

  return (
    <section className="flex flex-col gap-4">
      <Link to="/learn" className="text-sm text-brand hover:underline">
        ← Cours
      </Link>
      <h1 className="text-2xl font-bold">Le cours de {subject.data?.title}</h1>
      {path.isPending && <p>Chargement du parcours…</p>}
      {path.isError && !(path.error instanceof ApiError && path.error.status === 404) && (
        <p role="alert">Impossible de charger le parcours.</p>
      )}
      {path.isSuccess && domains.length === 0 && (
        <p className="text-ink-muted">
          Ce sujet n’a pas encore de référentiel de notions : ses cartes se révisent dans l’ordre de
          son document.
        </p>
      )}
      {path.isSuccess && domains.length > 0 && (
        <>
          <div className="flex flex-col gap-2 rounded-xl bg-surface p-4 shadow-sm">
            <p className="text-sm text-ink-muted">
              Parcours <strong>{track?.label}</strong> ·{' '}
              <Link to="/profile/subjects" className="text-brand hover:underline">
                changer de parcours
              </Link>
            </p>
            <MasteryBar counts={path.data.counts} label={subject.data?.title ?? slug} />
            <p className="text-sm font-medium">{masterySummary(path.data.counts)}</p>
          </div>
          {domains.map((domain) => (
            <DomainSection
              key={domain.id}
              subject={slug}
              domain={domain}
              open={domain.id === current}
            />
          ))}
        </>
      )}
    </section>
  )
}

/** Le domaine où l'on en est : le premier qui a encore une notion à acquérir, ou le seul. */
function currentDomain(domains: PathDomain[]): string | undefined {
  if (domains.length === 1) return domains[0].id
  return domains.find((domain) => domain.counts.acquired < domain.counts.total)?.id
}

function DomainSection({
  subject,
  domain,
  open,
}: {
  subject: string
  domain: PathDomain
  open: boolean
}) {
  const notions = domain.chapters.reduce((count, chapter) => count + chapter.notions.length, 0)
  // Dans le domaine, seul le chapitre où l'on en est s'ouvre d'emblée (audit, élément 5) : le premier qui a encore
  // une notion à acquérir, sinon le premier.
  const currentChapter = (
    domain.chapters.find((chapter) =>
      chapter.notions.some((notion) => notion.mastery === 'NEW' || notion.mastery === 'DISCOVERED'),
    ) ?? domain.chapters[0]
  )?.id
  return (
    <section
      aria-labelledby={`domain-${domain.id}`}
      className="flex flex-col gap-3 rounded-xl bg-surface p-4 shadow-sm"
    >
      <h2 id={`domain-${domain.id}`} className="text-lg font-semibold">
        {domain.title}
      </h2>
      <MasteryBar counts={domain.counts} label={domain.title} />
      <p className="text-xs text-ink-muted">{masterySummary(domain.counts)}</p>
      <details open={open}>
        <summary className="cursor-pointer text-sm font-medium text-brand-strong">
          {plural(domain.chapters.length, 'chapitre')} · {plural(notions, 'notion')}
        </summary>
        <div className="mt-3 flex flex-col gap-4">
          {domain.chapters.map((chapter) => {
            const mastered = isMastered(chapter.notions)
            return (
              <section key={chapter.id} aria-labelledby={`chapter-${chapter.id}`}>
                <details open={chapter.id === currentChapter}>
                  <summary className="cursor-pointer">
                    <h3 id={`chapter-${chapter.id}`} className="inline font-semibold">
                      {chapter.title}
                    </h3>
                    <span className="text-xs text-ink-muted">
                      {' · '}
                      {plural(chapter.notions.length, 'notion')}
                    </span>
                    {mastered && (
                      <span className="ml-2 inline-flex items-center gap-1 text-xs font-medium text-success">
                        <Trophy weight="fill" aria-hidden className="text-sun" /> Maîtrisé
                      </span>
                    )}
                  </summary>
                  <div className="mt-2 flex flex-col gap-2">
                    {mastered && (
                      <span className="self-start">
                        <ShareButton
                          card={{
                            kicker: 'Carte des compétences',
                            title: 'Chapitre maîtrisé',
                            detail: chapter.title,
                            url: `${window.location.origin}/subjects/${encodeURIComponent(subject)}`,
                          }}
                        />
                      </span>
                    )}
                    <ul aria-label={chapter.title} className="flex flex-col gap-1">
                      {chapter.notions.map((notion) => (
                        <NotionItem key={notion.slug} subject={subject} notion={notion} />
                      ))}
                    </ul>
                  </div>
                </details>
              </section>
            )
          })}
        </div>
      </details>
    </section>
  )
}

/** Un chapitre est maîtrisé quand chacune de ses notions l'est, ou mieux (ADR 0034). */
function isMastered(notions: PathNotion[]) {
  return (
    notions.length > 0 &&
    notions.every((notion) => notion.mastery === 'MASTERED' || notion.mastery === 'EXPERT')
  )
}

function NotionItem({ subject, notion }: { subject: string; notion: PathNotion }) {
  const blocked = notion.status === 'LOCKED' || notion.status === 'NO_CARDS'
  const badge = blocked
    ? {
        label: notion.status === 'LOCKED' ? 'Verrouillée' : 'À venir',
        style: 'bg-surface-muted text-ink-faint',
      }
    : { label: MASTERY_LABELS[notion.mastery], style: MASTERY_STYLES[notion.mastery] }
  return (
    <li className="flex items-center justify-between gap-3 rounded-lg border border-line px-3 py-2">
      <span className="flex flex-col">
        <Link
          to={`/subjects/${encodeURIComponent(subject)}/course/${encodeURIComponent(notion.slug)}`}
          className="font-medium hover:text-brand hover:underline"
        >
          {notion.title}
        </Link>
        <span className="text-xs text-ink-muted">
          {LEVEL_LABELS[notion.level]} · {notion.seenCount} / {notion.cardCount} cartes vues
        </span>
        {!blocked && notion.nextLevel && (
          <span className="text-xs text-ink-muted">{nextLevelHint(notion.nextLevel)}</span>
        )}
        {notion.status !== 'NO_CARDS' && (
          <Link
            to={practicePath(subject, notion.slug)}
            className="text-xs font-medium text-brand-strong hover:underline"
          >
            S’entraîner
          </Link>
        )}
      </span>
      <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${badge.style}`}>
        {badge.label}
      </span>
    </li>
  )
}
