import { ArrowClockwise, CaretRight } from '@phosphor-icons/react'
import { Link } from 'react-router'
import { eyebrow } from '../components/ui/typography'

/**
 * Les révisions du jour, sur l'accueil (ADR 0077) : les cartes à revoir maintenant, tous sujets suivis confondus, et un
 * lien vers la session de révision. Une ligne discrète au-dessus de la carte dominante sur un téléphone ; un bloc vert,
 * avec son nombre en grand, dans la colonne de droite d'un grand écran. C'est le même lien : seule sa forme change.
 */
export function DueReviewsCard({ due }: { due: number }) {
  return (
    <Link
      to="/review"
      className="flex items-center gap-3 rounded-xl bg-surface px-3.5 py-2.5 shadow-float motion-safe:transition-transform motion-safe:duration-75 motion-safe:active:translate-y-1 xl:col-start-2 xl:row-start-1 xl:flex-col xl:items-start xl:gap-1 xl:rounded-3xl xl:bg-success xl:p-5.5 xl:text-on-success xl:shadow-press-success"
    >
      <span
        aria-hidden="true"
        className="flex size-10 shrink-0 items-center justify-center rounded-[0.8125rem] bg-success-soft text-success-strong xl:hidden"
      >
        <ArrowClockwise weight="bold" className="size-6" />
      </span>
      <span className="flex flex-1 flex-col xl:gap-1">
        <span className={`hidden xl:block ${eyebrow()}`}>Révisions du jour</span>
        <span className="font-black xl:flex xl:flex-col xl:gap-1">
          <span className="xl:text-[3.25rem] xl:leading-none xl:tracking-[-0.09375rem]">{due}</span>{' '}
          <span className="xl:font-extrabold">carte{due > 1 ? 's' : ''} à revoir</span>
        </span>
        <span className="text-[0.8125rem] font-bold text-ink-muted xl:hidden">
          Quelques minutes, pour ne rien oublier
        </span>
      </span>
      <CaretRight weight="bold" aria-hidden className="size-5 shrink-0 text-ink-muted xl:hidden" />
    </Link>
  )
}
