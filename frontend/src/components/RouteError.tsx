import { Link, useRouteError } from 'react-router'
import { button } from './ui/button'

/**
 * Le filet d'une page (audit de novembre 2026, élément 8) : une erreur JavaScript, ou le code d'une page qui ne se
 * télécharge pas, affiche un message et de quoi repartir, au lieu d'un écran blanc. La navigation reste en place.
 */
export function PageError() {
  const error = useRouteError()
  // Pour qui ouvre la console ; rien n'est envoyé ailleurs.
  console.error(error)
  return (
    <section role="alert" className="flex flex-col items-start gap-4">
      <h1 className="text-2xl font-bold">Quelque chose s’est mal passé</h1>
      <p className="text-ink-muted">
        Cette page n’a pas pu s’afficher. Ta progression est enregistrée : recharge la page, ou
        reviens à l’accueil.
      </p>
      <div className="flex flex-wrap gap-3">
        <button type="button" onClick={() => window.location.reload()} className={button()}>
          Recharger
        </button>
        <a href="/" className={button({ variant: 'ghost' })}>
          Revenir à l’accueil
        </a>
      </div>
    </section>
  )
}

/** Le dernier filet, quand c'est la navigation elle-même qui a échoué : la même aide, sans elle. */
export function AppError() {
  return (
    <main className="mx-auto min-h-dvh max-w-3xl bg-canvas px-4 pt-10 text-ink">
      <PageError />
    </main>
  )
}

/** Une adresse qui ne mène à aucune page. */
export function NotFoundPage() {
  return (
    <section className="flex flex-col items-start gap-4">
      <h1 className="text-2xl font-bold">Page introuvable</h1>
      <p className="text-ink-muted">Cette adresse ne mène nulle part.</p>
      <Link to="/" className={button()}>
        Revenir à l’accueil
      </Link>
    </section>
  )
}
