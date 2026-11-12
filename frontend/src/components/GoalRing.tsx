const RADIUS = 32
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

/**
 * L'anneau de l'objectif du jour (ADR 0050) : il se remplit à chaque carte révisée, et devient plein quand l'objectif
 * est atteint. Plus l'anneau est près d'être plein, plus on a envie de le finir.
 */
export function GoalRing({
  done,
  goal,
  onBrand = false,
}: {
  done: number
  goal: number
  /** Posé sur la couleur de la marque (la carte « Aujourd'hui ») : l'anneau prend la couleur de son texte. */
  onBrand?: boolean
}) {
  const reached = Math.min(done, goal)
  const ratio = goal > 0 ? reached / goal : 0
  return (
    <svg
      viewBox="0 0 80 80"
      role="img"
      aria-label={`Objectif du jour : ${reached} cartes sur ${goal}`}
      className="size-20 shrink-0"
    >
      <circle
        cx="40"
        cy="40"
        r={RADIUS}
        fill="none"
        strokeWidth="10"
        className={onBrand ? 'stroke-brand-press' : 'stroke-surface-sunken'}
      />
      <circle
        cx="40"
        cy="40"
        r={RADIUS}
        fill="none"
        strokeWidth="10"
        strokeLinecap="round"
        strokeDasharray={CIRCUMFERENCE}
        strokeDashoffset={CIRCUMFERENCE * (1 - ratio)}
        transform="rotate(-90 40 40)"
        className={`${onBrand ? 'stroke-on-brand' : 'stroke-brand'} transition-[stroke-dashoffset] duration-500 motion-reduce:transition-none`}
      />
      <text
        x="40"
        y="46"
        textAnchor="middle"
        className={`${onBrand ? 'fill-on-brand' : 'fill-ink'} text-[17px] font-bold`}
        aria-hidden="true"
      >
        {reached}/{goal}
      </text>
    </svg>
  )
}
