import type { ReactNode, Ref } from 'react'
import { AnswerVerdict } from '../components/AnswerVerdict'
import { button } from '../components/ui/button'

/**
 * Le panneau du verdict d'une carte de révision (ADR 0058). Sur un téléphone, il est collé en bas de l'écran, sur toute
 * sa largeur, coins hauts arrondis : vert pâle pour une réponse juste, aux couleurs de l'erreur sinon. Rico, le
 * verdict (une icône et un texte, jamais la seule couleur), l'explication, puis « Carte suivante » en pleine largeur,
 * qui reçoit le focus. Sur un écran large, il reste au bas de la carte.
 *
 * Il se borne à la hauteur de l'écran et défile s'il le faut : une longue explication ne cache pas le bouton.
 *
 * @param detail une précision sous le verdict (« les bonnes réponses sont marquées »)
 * @param links les liens du bas du panneau : « Revoir la leçon », « Signaler une erreur »
 */
export function VerdictPanel({
  correct,
  title,
  detail,
  nextRef,
  onNext,
  links,
  children,
}: {
  correct: boolean
  title: string
  detail?: string
  nextRef: Ref<HTMLButtonElement>
  onNext: () => void
  links?: ReactNode
  children: ReactNode
}) {
  return (
    <div
      className={`sticky bottom-0 -mx-4 mt-auto flex max-h-[85dvh] flex-col gap-3 overflow-y-auto rounded-t-3xl px-5 pt-4.5 pb-[max(1.25rem,env(safe-area-inset-bottom))] motion-safe:animate-enter md:static md:mx-0 md:mt-0 md:max-h-none md:rounded-2xl md:border-2 md:p-5 ${correct ? 'bg-success-soft md:border-success-line' : 'bg-danger-soft md:border-danger-line'}`}
    >
      <AnswerVerdict correct={correct} detail={detail}>
        {title}
      </AnswerVerdict>
      <div className="text-sm font-bold">{children}</div>
      <button
        ref={nextRef}
        type="button"
        onClick={onNext}
        className={`w-full shrink-0 md:w-auto md:min-w-56 md:self-start ${button({ variant: correct ? 'success' : 'primary', size: 'lg' })}`}
      >
        Carte suivante
      </button>
      {links && (
        <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1">{links}</div>
      )}
    </div>
  )
}
