/**
 * Les morceaux de Rico que ses poses partagent (ADR 0077) : un bras, une main, une joue, un œil, un trait du visage.
 * Chacun porte ses tokens ; une pose ne donne que des positions et des chemins.
 */

export function Arm({ d }: { d: string }) {
  return <path d={d} strokeWidth="9" strokeLinecap="round" className="fill-none stroke-rico-limb" />
}

export function Hand({ x, y }: { x: number; y: number }) {
  return <circle cx={x} cy={y} r="7" className="fill-rico-limb" />
}

export function Cheek({ x, y }: { x: number; y: number }) {
  return <circle cx={x} cy={y} r="5" className="fill-sun" />
}

export function Cheeks({ y }: { y: number }) {
  return (
    <>
      <Cheek x={30} y={y} />
      <Cheek x={90} y={y} />
    </>
  )
}

/**
 * Un œil ouvert, qui cligne de temps en temps : son blanc en `x`, `y`, et sa pupille là où il regarde. Écarquillé
 * (`wide`), il est plus grand, la pupille plus petite et sans reflet.
 */
export function Eye({
  x,
  y = 56,
  look: [pupilX, pupilY],
  wide = false,
}: {
  x: number
  y?: number
  look: readonly [number, number]
  wide?: boolean
}) {
  return (
    <g className="origin-center [transform-box:fill-box] motion-safe:animate-blink">
      <circle cx={x} cy={y} r={wide ? 13 : 12} className="fill-white" />
      <circle cx={pupilX} cy={pupilY} r={wide ? 4 : 5.5} className="fill-rico-ink" />
      {!wide && <circle cx={pupilX + 2} cy={pupilY - 2} r="1.8" className="fill-white" />}
    </g>
  )
}

/** Un trait blanc du visage : une bouche fermée, ou des yeux plissés (plus épais). */
export function Line({ d, width = 4 }: { d: string; width?: number }) {
  return (
    <path
      d={d}
      strokeWidth={width}
      strokeLinecap="round"
      strokeLinejoin="round"
      className="fill-none stroke-white"
    />
  )
}

/** Une bouche ouverte, pleine. */
export function Grin({ d }: { d: string }) {
  return <path d={d} className="fill-white" />
}

export function Brow({ d }: { d: string }) {
  return <path d={d} strokeWidth="4" strokeLinecap="round" className="fill-none stroke-rico-limb" />
}
