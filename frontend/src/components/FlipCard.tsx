import { useEffect, useRef, type ReactNode } from 'react'

/**
 * Une carte qui se retourne d'un demi-tour (ADR 0048) : le recto, puis le verso. Les deux faces occupent la même case de
 * grille, la carte prend donc la hauteur de la plus haute. Le verso n'existe qu'une fois la carte retournée ; le recto
 * sort alors de l'arbre d'accessibilité, et le focus passe au verso pour qu'on continue au clavier. Sans animation pour
 * qui a demandé moins de mouvement à son système.
 */
export function FlipCard({
  flipped,
  front,
  back,
}: {
  flipped: boolean
  front: ReactNode
  back: ReactNode
}) {
  const backFace = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (flipped) backFace.current?.focus({ preventScroll: true })
  }, [flipped])

  return (
    <div className="perspective-distant">
      <div
        className={`grid transition-transform duration-500 transform-3d motion-reduce:transition-none ${flipped ? 'rotate-y-180' : ''}`}
      >
        <div
          className="col-start-1 row-start-1 backface-hidden"
          aria-hidden={flipped || undefined}
          inert={flipped}
        >
          {front}
        </div>
        {flipped && (
          <div
            ref={backFace}
            tabIndex={-1}
            className="col-start-1 row-start-1 rotate-y-180 outline-none backface-hidden"
          >
            {back}
          </div>
        )}
      </div>
    </div>
  )
}
