import { Link, useParams } from 'react-router'
import { useSubject } from '../api/content'
import { ApiError } from '../api/errors'
import { useLearningPath, type PathDomain, type PathNotion } from '../api/learningPath'
import { MasteryBar } from '../components/MasteryBar'
import {
  MASTERY_LABELS,
  MASTERY_STYLES,
  masterySummary,
  nextLevelHint,
} from '../components/mastery'
import { TRACKS } from '../components/trackLabels'

const LEVEL_LABELS: Record<PathNotion['level'], string> = {
  BEGINNER: 'débutant',
  INTERMEDIATE: 'intermédiaire',
  EXPERT: 'expert',
}

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

/**
 * La carte des compétences d'un sujet : ses domaines, chacun avec la maîtrise de ses notions (ADR 0034), puis ses
 * chapitres et leurs notions. En parcours « apprendre », une notion se débloque quand ses prérequis sont acquis
 * (ADR 0032). Le domaine où l'on en est se déplie d'emblée.
 */
export function LearningPathPage() {
  const { slug = '' } = useParams()
  const subject = useSubject(slug)
  const path = useLearningPath(slug)

  if (path.error instanceof ApiError && path.error.status === 404) {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-bold">Sujet introuvable</h1>
        <Link to="/subjects" className="text-indigo-600 hover:underline">
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
      <Link to={`/subjects/${slug}`} className="text-sm text-indigo-600 hover:underline">
        ← {subject.data?.title ?? 'Le sujet'}
      </Link>
      <h1 className="text-2xl font-bold">Mon parcours {subject.data?.title}</h1>
      {path.isPending && <p>Chargement du parcours…</p>}
      {path.isError && !(path.error instanceof ApiError && path.error.status === 404) && (
        <p role="alert">Impossible de charger le parcours.</p>
      )}
      {path.isSuccess && domains.length === 0 && (
        <p className="text-slate-600">
          Ce sujet n’a pas encore de référentiel de notions : ses cartes se révisent dans l’ordre de
          son document.
        </p>
      )}
      {path.isSuccess && domains.length > 0 && (
        <>
          <div className="flex flex-col gap-2 rounded-xl bg-white p-4 shadow-sm">
            <p className="text-sm text-slate-600">
              Parcours <strong>{track?.label}</strong> ·{' '}
              <Link to="/profile" className="text-indigo-600 hover:underline">
                changer de parcours
              </Link>
            </p>
            <MasteryBar counts={path.data.counts} label={subject.data?.title ?? slug} />
            <p className="text-sm font-medium">{masterySummary(path.data.counts)}</p>
          </div>
          {domains.map((domain) => (
            <DomainSection key={domain.id} domain={domain} open={domain.id === current} />
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

function DomainSection({ domain, open }: { domain: PathDomain; open: boolean }) {
  const notions = domain.chapters.reduce((count, chapter) => count + chapter.notions.length, 0)
  return (
    <section
      aria-labelledby={`domain-${domain.id}`}
      className="flex flex-col gap-3 rounded-xl bg-white p-4 shadow-sm"
    >
      <h2 id={`domain-${domain.id}`} className="text-lg font-semibold">
        {domain.title}
      </h2>
      <MasteryBar counts={domain.counts} label={domain.title} />
      <p className="text-xs text-slate-500">{masterySummary(domain.counts)}</p>
      <details open={open}>
        <summary className="cursor-pointer text-sm font-medium text-indigo-700">
          {plural(domain.chapters.length, 'chapitre')} · {plural(notions, 'notion')}
        </summary>
        <div className="mt-3 flex flex-col gap-4">
          {domain.chapters.map((chapter) => (
            <section
              key={chapter.id}
              aria-labelledby={`chapter-${chapter.id}`}
              className="flex flex-col gap-2"
            >
              <h3 id={`chapter-${chapter.id}`} className="font-semibold">
                {chapter.title}
              </h3>
              <ul className="flex flex-col gap-1">
                {chapter.notions.map((notion) => (
                  <NotionItem key={notion.slug} notion={notion} />
                ))}
              </ul>
            </section>
          ))}
        </div>
      </details>
    </section>
  )
}

function NotionItem({ notion }: { notion: PathNotion }) {
  const blocked = notion.status === 'LOCKED' || notion.status === 'NO_CARDS'
  const badge = blocked
    ? {
        label: notion.status === 'LOCKED' ? 'Verrouillée' : 'À venir',
        style: 'bg-slate-100 text-slate-400',
      }
    : { label: MASTERY_LABELS[notion.mastery], style: MASTERY_STYLES[notion.mastery] }
  return (
    <li className="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-3 py-2">
      <span className="flex flex-col">
        <span>{notion.title}</span>
        <span className="text-xs text-slate-500">
          {LEVEL_LABELS[notion.level]} · {notion.seenCount} / {notion.cardCount} cartes vues
        </span>
        {!blocked && notion.nextLevel && (
          <span className="text-xs text-slate-500">{nextLevelHint(notion.nextLevel)}</span>
        )}
      </span>
      <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${badge.style}`}>
        {badge.label}
      </span>
    </li>
  )
}
