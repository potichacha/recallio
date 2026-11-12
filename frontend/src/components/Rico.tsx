/** Les humeurs de Rico : ce que dit son visage, et son nom pour un lecteur d'écran. */
const MOODS = {
  happy: { label: 'Rico est content', mouth: 'M27 50c5 5 13 5 18 0' },
  cheering: { label: 'Rico t’encourage', mouth: 'M27 48c3 9 15 9 18 0z' },
  thinking: { label: 'Rico réfléchit', mouth: 'M29 52h14' },
} as const

export type RicoMood = keyof typeof MOODS

/** Le geste de chaque humeur : il sautille quand il encourage, penche la tête quand il réfléchit. */
const MOTION: Record<RicoMood, string> = {
  happy: '',
  cheering: 'motion-safe:animate-hop',
  thinking: 'motion-safe:animate-tilt',
}

/**
 * Rico, la mascotte (ADR 0058) : une carte violette aux oreilles pointues, qui salue, encourage ou réfléchit. Il est
 * dessiné avec les tokens, donc dans les deux thèmes. Il bouge un peu, et cligne des yeux ; pas pour qui a demandé
 * moins d'animations. C'est un dessin de principe : un illustrateur fera mieux, et le remplacer ne touche que ce
 * fichier.
 *
 * `decorative` le cache aux lecteurs d'écran quand le texte voisin dit déjà tout.
 */
export function Rico({
  mood = 'happy',
  size = 72,
  decorative = false,
}: {
  mood?: RicoMood
  size?: number
  decorative?: boolean
}) {
  const { label, mouth } = MOODS[mood]
  const pupilY = mood === 'thinking' ? 31.5 : 35.5
  return (
    <svg
      role={decorative ? undefined : 'img'}
      aria-label={decorative ? undefined : label}
      aria-hidden={decorative || undefined}
      width={size}
      height={size}
      viewBox="0 0 72 72"
      className={`shrink-0 origin-bottom ${MOTION[mood]}`}
    >
      <rect x="6" y="10" width="60" height="58" rx="20" className="fill-brand" />
      <path d="M18 10l6-9 7 9M41 10l7-9 6 9" className="fill-brand" />
      {/* Les yeux clignent de temps en temps ; les pupilles regardent en haut quand il réfléchit. */}
      <g className="origin-center [transform-box:fill-box] motion-safe:animate-blink">
        <circle cx="26" cy="34" r="8" className="fill-on-brand" />
        <circle cx="46" cy="34" r="8" className="fill-on-brand" />
        <circle cx="27.5" cy={pupilY} r="3.6" className="fill-brand-press" />
        <circle cx="47.5" cy={pupilY} r="3.6" className="fill-brand-press" />
      </g>
      <path
        d={mouth}
        strokeWidth="3.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        className={
          mood === 'cheering' ? 'fill-on-brand stroke-on-brand' : 'fill-none stroke-on-brand'
        }
      />
    </svg>
  )
}
