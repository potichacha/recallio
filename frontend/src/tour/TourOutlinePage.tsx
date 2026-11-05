import { Link, useParams } from 'react-router'
import { ApiError } from '../api/errors'
import { useTour, useTourProgress } from '../api/tour'
import { useSession } from '../auth/session'
import { ChapterExercises } from './ChapterExercises'
import { tourPath, tourSteps } from './tourSteps'
import { button } from '../components/ui/button'

const PRIMARY_LINK = `self-start ${button()}`

/**
 * Le sommaire du Tour d'un sujet (ADR 0037) : ses domaines, ses chapitres et ses notions, dans l'ordre où on les
 * découvre. Public, comme le catalogue.
 */
export function TourOutlinePage() {
  const { slug = '' } = useParams()
  const tour = useTour(slug)
  const session = useSession()
  const progress = useTourProgress(slug, session.status === 'authenticated')
  const validated = new Set(progress.data?.validatedNotions)

  if (tour.error instanceof ApiError && tour.error.status === 404) {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-bold">Sujet introuvable</h1>
        <Link to="/subjects" className="text-brand hover:underline">
          Retour aux sujets
        </Link>
      </section>
    )
  }
  if (tour.isError) return <p role="alert">Impossible de charger le Tour.</p>
  if (!tour.data) return <p className="text-ink-muted">Chargement…</p>

  const { subjectTitle, domains } = tour.data
  const first = tourSteps(tour.data)[0]
  const notions = domains.flatMap((domain) => domain.chapters).flatMap((chapter) => chapter.notions)
  // Une notion validée puis sortie du Tour ne compte plus.
  const validatedCount = notions.filter((notion) => validated.has(notion.slug)).length
  const lastPage = progress.data?.lastPage
  const resumed = lastPage ? notions.find((notion) => notion.slug === lastPage.notion) : undefined

  return (
    <section className="flex flex-col gap-6">
      <Link
        to={`/subjects/${encodeURIComponent(slug)}`}
        className="text-sm text-brand hover:underline"
      >
        ← {subjectTitle}
      </Link>
      <div className="flex flex-col gap-3">
        <h1 className="text-2xl font-bold">Le Tour de {subjectTitle}</h1>
        {first ? (
          <>
            <p className="text-ink-muted">
              Apprends {subjectTitle} pas à pas. Chaque page explique une notion et propose un
              programme à modifier et à exécuter.
            </p>
            {lastPage && resumed ? (
              <>
                <Link to={tourPath(slug, lastPage)} className={PRIMARY_LINK}>
                  Reprendre le Tour
                </Link>
                <p className="text-sm text-ink-muted">
                  Tu en étais à « {resumed.title} »
                  {resumed.pageCount > 1 && `, page ${lastPage.page} sur ${resumed.pageCount}`}.
                </p>
              </>
            ) : (
              <Link to={tourPath(slug, first)} className={PRIMARY_LINK}>
                Commencer le Tour
              </Link>
            )}
            {progress.data && (
              <p className="text-sm text-ink-muted">
                {validatedCount} / {notions.length} notion{notions.length > 1 ? 's' : ''} validée
                {notions.length > 1 ? 's' : ''}
              </p>
            )}
          </>
        ) : (
          <p className="text-ink-muted">Ce sujet n’a pas encore de Tour.</p>
        )}
      </div>
      {domains.map((domain) => (
        <section
          key={domain.id}
          aria-labelledby={`tour-${domain.id}`}
          className="flex flex-col gap-3"
        >
          <h2 id={`tour-${domain.id}`} className="text-lg font-semibold">
            {domain.title}
          </h2>
          {domain.chapters.map((chapter) => (
            <div
              key={chapter.id}
              className="flex flex-col gap-2 rounded-xl bg-surface p-4 shadow-sm"
            >
              <h3 className="font-medium text-ink-soft">{chapter.title}</h3>
              <ol aria-label={chapter.title} className="flex flex-col gap-1">
                {chapter.notions.map((notion) => (
                  <li key={notion.slug}>
                    <Link
                      to={tourPath(slug, { notion: notion.slug, page: 1 })}
                      className="text-brand-strong hover:underline"
                    >
                      {notion.title}
                    </Link>
                    {notion.pageCount > 1 && (
                      <span className="text-sm text-ink-muted"> · {notion.pageCount} pages</span>
                    )}
                    {validated.has(notion.slug) && (
                      <span className="text-sm font-medium text-success"> · validée</span>
                    )}
                  </li>
                ))}
              </ol>
              {chapter.exercises.length > 0 && (
                <ChapterExercises
                  subject={slug}
                  title={chapter.title}
                  exercises={chapter.exercises}
                />
              )}
            </div>
          ))}
        </section>
      ))}
    </section>
  )
}
