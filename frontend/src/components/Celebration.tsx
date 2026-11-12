import type { CSSProperties } from 'react'

const PIECES = 18
const COLOURS = ['bg-brand', 'bg-sun', 'bg-success', 'bg-flame']

/** Où retombe chaque pièce, calculé depuis son rang : le même dessin à chaque fois, sans hasard dans le rendu. */
function landing(index: number): CSSProperties {
  const angle = (index / PIECES) * 2 * Math.PI
  const reach = 70 + (index % 3) * 28
  return {
    '--confetti-x': `${Math.round(Math.cos(angle) * reach)}px`,
    '--confetti-y': `${Math.round(Math.sin(angle) * reach * 0.7 - 30)}px`,
    '--confetti-turn': `${(index % 2 === 0 ? 1 : -1) * (180 + index * 20)}deg`,
    animationDelay: `${(index % 4) * 40}ms`,
  } as CSSProperties
}

/**
 * Une gerbe de confettis (ADR 0058), pour les moments qui comptent : l'objectif du jour atteint, une session sans
 * faute, une notion validée, une promotion. Elle dure une seconde, ne capte aucun clic, ne se rejoue pas, et disparaît
 * pour qui a demandé moins d'animations : le texte voisin dit déjà la nouvelle. À poser dans un parent `relative`.
 */
export function Celebration() {
  return (
    <span
      aria-hidden="true"
      className="pointer-events-none absolute top-1/2 left-1/2 motion-reduce:hidden"
    >
      {Array.from({ length: PIECES }, (_, index) => (
        <span
          key={index}
          style={landing(index)}
          className={`absolute size-2 rounded-sm opacity-0 motion-safe:animate-confetti ${COLOURS[index % COLOURS.length]}`}
        />
      ))}
    </span>
  )
}
