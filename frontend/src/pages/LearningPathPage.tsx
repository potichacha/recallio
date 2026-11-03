import { Link, useParams } from 'react-router'
import { useSubject } from '../api/content'
import { ApiError } from '../api/errors'
import { useLearningPath, type NotionStatus, type PathNotion } from '../api/learningPath'
import { TRACKS } from '../components/trackLabels'

const STATUS_LABELS: Record<NotionStatus, string> = {
  NO_CARDS: 'À venir',
  LOCKED: 'Verrouillée',
  AVAILABLE: 'Disponible',
  IN_PROGRESS: 'En cours',
  ACQUIRED: 'Acquise',
}

const STATUS_STYLES: Record<NotionStatus, string> = {
  NO_CARDS: 'bg-slate-100 text-slate-400',
  LOCKED: 'bg-slate-100 text-slate-500',
  AVAILABLE: 'bg-sky-50 text-sky-700',
  IN_PROGRESS: 'bg-amber-50 text-amber-700',
  ACQUIRED: 'bg-emerald-50 text-emerald-700',
}

const LEVEL_LABELS: Record<PathNotion['level'], string> = {
  BEGINNER: 'débutant',
  INTERMEDIATE: 'intermédiaire',
  EXPERT: 'expert',
}

/**
 * La carte du parcours d'un sujet (ADR 0032) : ses chapitres et leurs notions, chacune avec son état. En parcours
 * « apprendre », une notion se débloque quand ses prérequis sont acquis.
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

  const notions = path.data?.chapters.flatMap((chapter) => chapter.notions) ?? []
  const acquired = notions.filter((notion) => notion.status === 'ACQUIRED').length
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
      {path.isSuccess && notions.length === 0 && (
        <p className="text-slate-600">
          Ce sujet n’a pas encore de référentiel de notions : ses cartes se révisent dans l’ordre de
          son document.
        </p>
      )}
      {path.isSuccess && notions.length > 0 && (
        <>
          <p className="text-sm text-slate-600">
            Parcours <strong>{track?.label}</strong> ·{' '}
            <Link to="/profile" className="text-indigo-600 hover:underline">
              changer de parcours
            </Link>{' '}
            · {acquired} / {notions.length} notions acquises
          </p>
          {path.data.chapters.map((chapter) => (
            <section
              key={chapter.id}
              aria-labelledby={`chapter-${chapter.id}`}
              className="flex flex-col gap-2"
            >
              <h2 id={`chapter-${chapter.id}`} className="text-lg font-semibold">
                {chapter.title}
              </h2>
              <ul className="flex flex-col gap-1">
                {chapter.notions.map((notion) => (
                  <li
                    key={notion.slug}
                    className="flex items-center justify-between gap-3 rounded-lg border border-slate-200 bg-white px-3 py-2"
                  >
                    <span className="flex flex-col">
                      <span>{notion.title}</span>
                      <span className="text-xs text-slate-500">
                        {LEVEL_LABELS[notion.level]} · {notion.seenCount} / {notion.cardCount}{' '}
                        cartes vues
                      </span>
                    </span>
                    <span
                      className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[notion.status]}`}
                    >
                      {STATUS_LABELS[notion.status]}
                    </span>
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </>
      )}
    </section>
  )
}
