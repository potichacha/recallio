/** Les humeurs de Rico : ce que dit son visage, et son nom pour un lecteur d'écran. */
const MOODS = {
  happy: { label: 'Rico est content', mouth: 'M27 50c5 5 13 5 18 0' },
  cheering: { label: 'Rico t’encourage', mouth: 'M27 48c3 9 15 9 18 0z' },
  applauding: { label: 'Rico applaudit', mouth: 'M26 48c5 7 15 7 20 0z' },
  thinking: { label: 'Rico réfléchit', mouth: 'M29 52h14' },
} as const

export type RicoMood = keyof typeof MOODS

/** Le geste de chaque humeur : il sautille quand il encourage ou applaudit, penche la tête quand il réfléchit. */
const MOTION: Record<RicoMood, string> = {
  happy: '',
  cheering: 'motion-safe:animate-hop',
  applauding: 'motion-safe:animate-hop',
  thinking: 'motion-safe:animate-tilt',
}

/** Une bouche ouverte, pleine, pour encourager et applaudir ; un trait sinon. */
const OPEN_MOUTH: Record<RicoMood, boolean> = {
  happy: false,
  cheering: true,
  applauding: true,
  thinking: false,
}

/**
 * Rico, la mascotte (ADR 0058) : une carte violette aux oreilles pointues, qui salue, encourage, applaudit ou
 * réfléchit. Il est dessiné avec les tokens, donc dans les deux thèmes. Il bouge un peu, et cligne des yeux ; pas pour
 * qui a demandé moins d'animations. C'est un dessin de principe : un illustrateur fera mieux, et le remplacer ne touche
 * que ce fichier.
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
      <rect x="6" y="10" width="60" height="58" rx="20" className="fill-rico" />
      <path d="M18 10l6-9 7 9M41 10l7-9 6 9" className="fill-rico" />
      {mood === 'applauding' ? (
        // Les yeux plissés de qui applaudit.
        <path
          d="M19 35c3-5 10-5 13 0M40 35c3-5 10-5 13 0"
          strokeWidth="3.5"
          strokeLinecap="round"
          className="fill-none stroke-white"
        />
      ) : (
        // Les yeux clignent de temps en temps ; les pupilles regardent en haut quand il réfléchit.
        <g className="origin-center [transform-box:fill-box] motion-safe:animate-blink">
          <circle cx="26" cy="34" r="8" className="fill-white" />
          <circle cx="46" cy="34" r="8" className="fill-white" />
          <circle cx="27.5" cy={pupilY} r="3.6" className="fill-rico-ink" />
          <circle cx="47.5" cy={pupilY} r="3.6" className="fill-rico-ink" />
        </g>
      )}
      <path
        d={mouth}
        strokeWidth="3.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        className={OPEN_MOUTH[mood] ? 'fill-white stroke-white' : 'fill-none stroke-white'}
      />
    </svg>
  )
}

/**
 * Rico qui tient une carte, sur la page publique : le dessin de l'accroche. Décoratif, le titre voisin dit tout. Il ne
 * bouge pas : aucune action ne l'a provoqué.
 */
export function RicoWithCard() {
  return (
    <svg aria-hidden="true" width="190" height="150" viewBox="0 0 190 150" className="shrink-0">
      <g transform="rotate(12 146 71)">
        <rect x="118" y="34" width="56" height="74" rx="10" className="fill-sun" />
        <path
          d="M134 62h26M134 74h18"
          strokeWidth="4"
          strokeLinecap="round"
          className="stroke-on-sun"
        />
      </g>
      <rect x="22" y="30" width="112" height="120" rx="34" className="fill-rico" />
      <path d="M50 30l10-18 12 18M88 30l12-18 10 18" className="fill-rico" />
      <circle cx="60" cy="78" r="15" className="fill-white" />
      <circle cx="98" cy="78" r="15" className="fill-white" />
      <circle cx="63" cy="81" r="7" className="fill-rico-ink" />
      <circle cx="101" cy="81" r="7" className="fill-rico-ink" />
      <path
        d="M64 108c9 8 21 8 30 0"
        strokeWidth="5"
        strokeLinecap="round"
        className="fill-none stroke-white"
      />
    </svg>
  )
}
