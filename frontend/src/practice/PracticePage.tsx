import { EmptyState } from '../components/states/EmptyState'
import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { Link, useParams, useSearchParams } from 'react-router'
import { ApiError } from '../api/errors'
import { checkPracticeAnswer, usePractice } from '../api/practice'
import { PracticeDeck } from './PracticeDeck'

/**
 * S'entraîner sur une notion (ADR 0038) : toutes ses cartes, une à une, les formats mêlés. Rien n'est enregistré : la
 * révision espacée reprendra ces cartes, une par jour.
 */
export function PracticePage() {
  const { slug = '', notion = '' } = useParams()
  const [params] = useSearchParams()
  const practice = usePractice(slug, notion)
  const subjectPath = `/subjects/${encodeURIComponent(slug)}`
  const back =
    params.get('from') === 'tour'
      ? { to: `${subjectPath}/tour/${encodeURIComponent(notion)}`, label: 'Revenir au Tour' }
      : { to: `${subjectPath}/course`, label: 'Voir le cours' }

  if (practice.error instanceof ApiError && practice.error.status === 404) {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-bold">Notion introuvable</h1>
        <Link to={subjectPath} className="text-brand hover:underline">
          Retour au sujet
        </Link>
      </section>
    )
  }
  if (practice.isError) {
    return (
      <ErrorState
        message="Impossible de charger l’entraînement."
        onRetry={() => void practice.refetch()}
      />
    )
  }
  if (!practice.data) return <LoadingState label="Chargement…" shape="card" />

  const { notionTitle, cards } = practice.data
  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">S’entraîner : {notionTitle}</h1>
        <p className="text-sm text-ink-muted">
          Rien n’est enregistré : ces cartes reviendront dans tes révisions, une par jour.
        </p>
      </header>
      {cards.length === 0 && (
        <EmptyState title="Cette notion n’a pas encore de cartes." mood="thinking" />
      )}
      <PracticeDeck
        subject={slug}
        cards={cards}
        check={(answer) => checkPracticeAnswer(slug, notion, answer)}
        startAt={params.get('card')}
        doneLabel="Entraînement terminé !"
      />
      <Link to={back.to} className="self-start text-sm text-brand hover:underline">
        {back.label}
      </Link>
    </section>
  )
}
