import { Link, useParams } from 'react-router'
import { useCatalogSubjects } from '../api/content'
import { checkMistakeAnswer, useMistakes } from '../api/mistakes'
import { PracticeDeck } from './PracticeDeck'

/**
 * Le carnet d'erreurs d'un sujet (ADR 0057) : les cartes ratées ces sept derniers jours et pas réussies depuis, à
 * refaire tout de suite. Rien n'est enregistré : elles reviennent aussi dans les révisions, à leur heure.
 */
export function MistakesPage() {
  const { slug = '' } = useParams()
  const mistakes = useMistakes(slug, { session: true })
  const subjects = useCatalogSubjects()
  const title = subjects.data?.find((subject) => subject.slug === slug)?.title ?? slug

  if (mistakes.isError) return <p role="alert">Impossible de charger tes erreurs.</p>
  if (!mistakes.data) return <p className="text-ink-muted">Chargement…</p>

  const { cards } = mistakes.data
  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">Mes erreurs de la semaine : {title}</h1>
        <p className="text-sm text-ink-muted">
          Les cartes que tu as ratées ces sept derniers jours, sans les avoir réussies depuis. Rien
          n’est enregistré : elles reviendront aussi dans tes révisions.
        </p>
      </header>
      {cards.length === 0 && <p>Aucune erreur à revoir cette semaine.</p>}
      <PracticeDeck
        subject={slug}
        cards={cards}
        check={(answer) => checkMistakeAnswer(slug, answer)}
        showNotion
        doneLabel="Erreurs revues !"
      />
      <Link
        to={`/review?subject=${encodeURIComponent(slug)}`}
        className="self-start text-sm text-brand hover:underline"
      >
        Revenir aux révisions
      </Link>
    </section>
  )
}
