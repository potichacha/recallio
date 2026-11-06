import { Link, useParams } from 'react-router'
import { useNotionLesson, useSubject } from '../api/content'
import { ApiError } from '../api/errors'
import { Markdown } from '../components/Markdown'
import { button } from '../components/ui/button'
import { practicePath } from '../practice/practicePath'

/**
 * La leçon d'une notion, dans le cours d'un sujet (ADR 0052) : on la lit, puis on s'entraîne, ou l'on passe à la notion
 * suivante du référentiel. Lisible sans compte, comme le catalogue.
 */
export function LessonPage() {
  const { slug = '', notion = '' } = useParams()
  const subject = useSubject(slug)
  const lesson = useNotionLesson(slug, notion)
  const course = `/subjects/${encodeURIComponent(slug)}/course`
  const notFound = lesson.error instanceof ApiError && lesson.error.status === 404

  return (
    <article className="flex flex-col gap-5">
      <Link to={course} className="self-start text-sm text-brand hover:underline">
        ← Le cours de {subject.data?.title ?? 'ce sujet'}
      </Link>
      {lesson.isPending && <p>Chargement de la leçon…</p>}
      {notFound && <h1 className="text-2xl font-bold">Notion introuvable</h1>}
      {lesson.isError && !notFound && <p role="alert">Impossible de charger la leçon.</p>}
      {lesson.data && (
        <>
          <header className="flex flex-col gap-1">
            <p className="text-sm text-ink-muted">{lesson.data.chapterTitle}</p>
            <h1 className="text-2xl font-bold">{lesson.data.title}</h1>
          </header>
          {lesson.data.lesson ? (
            <div className="rounded-xl border border-line bg-surface p-5 shadow-sm">
              <Markdown>{lesson.data.lesson}</Markdown>
            </div>
          ) : (
            <p className="text-ink-muted">
              Cette notion n’a pas encore de leçon : ses cartes t’attendent à l’entraînement.
            </p>
          )}
          <div className="flex flex-wrap gap-3">
            <Link to={practicePath(slug, notion)} className={button()}>
              S’entraîner sur cette notion
            </Link>
            {lesson.data.inTour && (
              <Link
                to={`/subjects/${encodeURIComponent(slug)}/tour/${encodeURIComponent(notion)}`}
                className={button({ variant: 'secondary' })}
              >
                Sa page dans le Tour
              </Link>
            )}
          </div>
          <nav
            aria-label="Leçons voisines"
            className="flex flex-wrap justify-between gap-3 text-sm"
          >
            {lesson.data.previous ? (
              <Link
                to={`${course}/${encodeURIComponent(lesson.data.previous.slug)}`}
                className="text-brand hover:underline"
              >
                ← {lesson.data.previous.title}
              </Link>
            ) : (
              <span />
            )}
            {lesson.data.next && (
              <Link
                to={`${course}/${encodeURIComponent(lesson.data.next.slug)}`}
                className="text-brand hover:underline"
              >
                {lesson.data.next.title} →
              </Link>
            )}
          </nav>
        </>
      )}
    </article>
  )
}
