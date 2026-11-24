const RADIUS = 30
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

/**
 * L'anneau de l'objectif du jour (ADR 0050) : il se remplit à chaque carte révisée, et devient plein quand l'objectif
 * est atteint. Plus l'anneau est près d'être plein, plus on a envie de le finir.
 */
export function GoalRing({
  done,
  goal,
  onHero = false,
}: {
  done: number
  goal: number
  /** Posé sur la carte « Aujourd'hui » : l'anneau se remplit en jaune, sur la piste de la carte. */
  onHero?: boolean
}) {
  const reached = Math.min(done, goal)
  const ratio = goal > 0 ? reached / goal : 0
  return (
    <svg
      viewBox="0 0 76 76"
      role="img"
      aria-label={`Objectif du jour : ${reached} cartes sur ${goal}`}
      className="size-19 shrink-0"
    >
      <circle
        cx="38"
        cy="38"
        r={RADIUS}
        fill="none"
        strokeWidth="10"
        className={onHero ? 'stroke-hero-track' : 'stroke-surface-sunken'}
      />
      <circle
        cx="38"
        cy="38"
        r={RADIUS}
        fill="none"
        strokeWidth="10"
        strokeLinecap="round"
        strokeDasharray={CIRCUMFERENCE}
        strokeDashoffset={CIRCUMFERENCE * (1 - ratio)}
        transform="rotate(-90 38 38)"
        className={`${onHero ? 'stroke-sun' : 'stroke-brand'} transition-[stroke-dashoffset] duration-500 motion-reduce:transition-none`}
      />
      <text
        x="38"
        y="44"
        textAnchor="middle"
        className={`${onHero ? 'fill-on-hero' : 'fill-ink'} text-lg font-black`}
        aria-hidden="true"
      >
        {reached}/{goal}
      </text>
    </svg>
  )
}
