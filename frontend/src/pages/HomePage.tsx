import { Cards, Code, Path } from '@phosphor-icons/react'
import { Link } from 'react-router'
import { useSubjects } from '../api/content'
import { useApiStatus } from '../api/health'
import { RequireFollowedSubjects } from '../auth/RequireFollowedSubjects'
import { useSession } from '../auth/session'
import { button } from '../components/ui/button'
import { DashboardPage } from '../dashboard/DashboardPage'

/** Accueil : le tableau de bord pour un utilisateur connecté (après le choix de ses sujets), la présentation sinon. */
export function HomePage() {
  const session = useSession()
  return session.status === 'authenticated' ? (
    <RequireFollowedSubjects>
      <DashboardPage />
    </RequireFollowedSubjects>
  ) : (
    <Landing />
  )
}

const PROMISES = [
  {
    icon: Cards,
    text: 'Des cartes d’entretien, corrigées ou à retourner, qui reviennent quand tu les oublies.',
  },
  { icon: Code, text: 'Des exercices de code jugés par leurs tests, dans ton navigateur.' },
  {
    icon: Path,
    text: 'Un Tour pour apprendre Java ou Go page par page, en exécutant chaque exemple.',
  },
]

/** La présentation, pour un visiteur (ADR 0050) : ce que l'application apporte, puis l'essayer tout de suite. */
function Landing() {
  const status = useApiStatus()
  const subjects = useSubjects()
  const session = useSession()
  const trials =
    subjects.data?.filter((subject) => subject.hasTour && subject.status === 'PUBLISHED') ?? []

  return (
    <section className="flex flex-col gap-8 py-4">
      <div className="flex flex-col gap-4">
        <h1 className="text-4xl leading-tight font-bold">
          Prépare tes entretiens techniques, dix minutes par jour.
        </h1>
        <ul className="flex flex-col gap-3">
          {PROMISES.map(({ icon: Icon, text }) => (
            <li key={text} className="flex items-start gap-3 text-lg text-ink-soft">
              <Icon weight="bold" aria-hidden className="mt-1 size-6 shrink-0 text-brand" />
              {text}
            </li>
          ))}
        </ul>
      </div>
      {trials.length > 0 && (
        <section
          aria-labelledby="trial-title"
          className="flex flex-col gap-3 rounded-2xl bg-surface p-5 shadow-sm"
        >
          <h2 id="trial-title" className="text-xl font-semibold">
            Essaie sans compte
          </h2>
          <p className="text-ink-muted">Cinq questions, corrigées tout de suite. Rien à remplir.</p>
          <div className="flex flex-wrap gap-3">
            {trials.map((subject) => (
              <Link key={subject.slug} to={`/try/${subject.slug}`} className={button()}>
                Essayer {subject.title}
              </Link>
            ))}
          </div>
        </section>
      )}
      <div className="flex flex-wrap gap-3">
        <button
          type="button"
          onClick={() => session.login('/')}
          className={button({ variant: 'secondary' })}
        >
          Me connecter ou créer un compte
        </button>
        <Link to="/learn" className={button({ variant: 'ghost' })}>
          Voir ce qu’on peut apprendre
        </Link>
      </div>
      <p role="status" className="text-xs text-ink-muted">
        {status.isPending && 'API : vérification…'}
        {status.isSuccess && `API : ${status.data}`}
        {status.isError && 'API : injoignable'}
      </p>
    </section>
  )
}
