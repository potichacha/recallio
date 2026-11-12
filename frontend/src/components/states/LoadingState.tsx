const BLOCK = 'rounded-lg bg-surface-sunken'

/** La silhouette de ce qui arrive : une page de texte, une grille de tuiles, une carte, ou une seule ligne. */
export type LoadingShape = 'page' | 'tiles' | 'card' | 'line'

const TILES = 6

function Silhouette({ shape }: { shape: LoadingShape }) {
  if (shape === 'line') return <div className={`${BLOCK} h-5 w-2/3`} />
  if (shape === 'tiles') {
    return (
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {Array.from({ length: TILES }, (_, index) => (
          <div key={index} className={`${BLOCK} h-16 rounded-xl`} />
        ))}
      </div>
    )
  }
  if (shape === 'card') {
    return (
      <div className="flex flex-col gap-3 rounded-2xl border border-line bg-surface p-5">
        <div className={`${BLOCK} h-4 w-1/3`} />
        <div className={`${BLOCK} h-5 w-full`} />
        <div className={`${BLOCK} h-5 w-4/5`} />
        <div className={`${BLOCK} h-11 w-full rounded-xl`} />
        <div className={`${BLOCK} h-11 w-full rounded-xl`} />
      </div>
    )
  }
  return (
    <div className="flex flex-col gap-3">
      <div className={`${BLOCK} h-8 w-1/2`} />
      <div className={`${BLOCK} h-4 w-full`} />
      <div className={`${BLOCK} h-4 w-5/6`} />
      <div className={`${BLOCK} h-4 w-2/3`} />
    </div>
  )
}

/**
 * Un écran ou un bloc qui charge (ADR 0058) : la silhouette grise de ce qui arrive, plutôt qu'une ligne de texte. Elle
 * n'apparaît qu'après un court délai, pour qu'une réponse rapide n'en montre aucune, et ne pulse pas pour qui a demandé
 * moins d'animations. La silhouette est muette : `label` dit aux lecteurs d'écran ce qui charge.
 */
export function LoadingState({
  label = 'Chargement…',
  shape = 'page',
}: {
  label?: string
  shape?: LoadingShape
}) {
  return (
    <div aria-busy="true">
      <p className="sr-only">{label}</p>
      <div aria-hidden="true" className="motion-safe:animate-skeleton motion-reduce:animate-appear">
        <Silhouette shape={shape} />
      </div>
    </div>
  )
}
