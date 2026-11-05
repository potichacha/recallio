import { Link, useLocation, useParams, useSearchParams } from 'react-router'
import { ApiError } from '../api/errors'
import { useTour, useTourNotion } from '../api/tour'
import { useSession } from '../auth/session'
import { Markdown } from '../components/Markdown'
import { NotionCheck } from './NotionCheck'
import { Playground } from './Playground'
import { neighbours, tourPath, tourSteps, type TourStep } from './tourSteps'

/**
 * Une page du Tour (ADR 0037) : son texte, puis son programme à modifier et à exécuter, puis les pages voisines. Le
 * rang de la page dans sa notion est dans l'adresse (`?page=2`), pour qu'un lien mène à la bonne page.
 */
export function TourNotionPage() {
  const { slug = '', notion = '' } = useParams()
  const [params] = useSearchParams()
  const pageNumber = Math.max(1, Number(params.get('page')) || 1)
  const outline = useTour(slug)
  const pages = useTourNotion(slug, notion)
  const tourLink = `/subjects/${encodeURIComponent(slug)}/tour`

  if (pages.isError) {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-bold">
          {pages.error instanceof ApiError && pages.error.status === 404
            ? 'Page introuvable'
            : 'Impossible de charger la page.'}
        </h1>
        <Link to={tourLink} className="text-indigo-600 hover:underline">
          Retour au sommaire du Tour
        </Link>
      </section>
    )
  }
  if (!pages.data) return <p className="text-slate-500">Chargement…</p>

  const page = pages.data.pages[pageNumber - 1]
  if (!page) {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-bold">Page introuvable</h1>
        <Link to={tourLink} className="text-indigo-600 hover:underline">
          Retour au sommaire du Tour
        </Link>
      </section>
    )
  }

  const steps = outline.data ? tourSteps(outline.data) : []
  const { index, previous, next } = neighbours(steps, { notion, page: pageNumber })

  return (
    <article className="flex flex-col gap-5">
      <nav aria-label="Fil d’Ariane" className="text-sm text-slate-500">
        <Link to={tourLink} className="text-indigo-600 hover:underline">
          Le Tour{outline.data ? ` de ${outline.data.subjectTitle}` : ''}
        </Link>
        {' › '}
        {pages.data.chapterTitle}
      </nav>
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">{page.title}</h1>
        {index >= 0 && (
          <p className="text-sm text-slate-500">
            Page {index + 1} sur {steps.length}
          </p>
        )}
      </header>
      <Markdown>{page.text}</Markdown>
      {/* Une page neuve, un programme neuf : la clé recrée l'éditeur et oublie la sortie de la page précédente. */}
      <Playground key={`${notion}/${pageNumber}`} files={page.example} />
      {pageNumber === pages.data.pages.length && <Validation subject={slug} notion={notion} />}
      <TourNavigation subject={slug} previous={previous} next={next} tourLink={tourLink} />
    </article>
  )
}

/** Les questions de la notion, sur sa dernière page : il faut être connecté pour les valider (ADR 0037). */
function Validation({ subject, notion }: { subject: string; notion: string }) {
  const session = useSession()
  const location = useLocation()
  if (session.status === 'authenticated') {
    return <NotionCheck key={notion} subject={subject} notion={notion} />
  }
  if (session.status !== 'anonymous') return null
  return (
    <section className="flex flex-col items-start gap-3 rounded-xl bg-white p-5 shadow-sm">
      <h2 className="text-lg font-semibold">Valider la notion</h2>
      <p className="text-sm text-slate-600">
        Connecte-toi pour répondre aux questions et suivre ta progression dans le Tour.
      </p>
      <button
        type="button"
        onClick={() => session.login(location.pathname + location.search)}
        className="rounded-lg border border-indigo-200 px-4 py-2 font-medium text-indigo-700 hover:bg-indigo-50"
      >
        Se connecter pour valider
      </button>
    </section>
  )
}

function TourNavigation({
  subject,
  previous,
  next,
  tourLink,
}: {
  subject: string
  previous: TourStep | undefined
  next: TourStep | undefined
  tourLink: string
}) {
  return (
    <nav
      aria-label="Pages du Tour"
      className="flex items-center justify-between gap-3 border-t border-slate-200 pt-4"
    >
      {previous ? (
        <Link
          to={tourPath(subject, previous)}
          aria-label="Page précédente"
          className="rounded-lg border border-slate-200 px-4 py-2 font-medium text-slate-700 hover:bg-slate-50"
        >
          ← Précédente
        </Link>
      ) : (
        <span />
      )}
      {next ? (
        <Link
          to={tourPath(subject, next)}
          aria-label="Page suivante"
          className="rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700"
        >
          Suivante →
        </Link>
      ) : (
        <Link to={tourLink} className="font-medium text-indigo-700 hover:underline">
          Retour au sommaire
        </Link>
      )}
    </nav>
  )
}
