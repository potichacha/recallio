import type { ReactNode } from 'react'
import { Arm, Brow, Cheek, Cheeks, Eye, Grin, Hand, Line } from './ricoParts'

/**
 * Les poses de Rico (ADR 0077), d'après la planche `docs/maquettes/trois-portes/RicoPoses.dc.html` : mêmes chemins,
 * mêmes proportions. Le corps n'est dessiné qu'une fois, dans `Rico.tsx` ; une pose n'apporte que ce qui lui est
 * propre : ce qui passe derrière le corps (`back`), puis son visage et ce qui passe devant (`front`), avec les
 * morceaux de `ricoParts.tsx`.
 */

/** Le cadre du dessin, comme un `viewBox` : x, y, largeur, hauteur. */
export type Frame = readonly [x: number, y: number, width: number, height: number]

/** Le cadre de la plupart des poses ; un accessoire tenu en l'air ou tendu sur le côté l'agrandit. */
export const STANDARD_FRAME: Frame = [-12, 0, 144, 122]

/** Les oreilles, et leur intérieur : dressées, tombantes quand il compatit ou dort, tendues quand il est surpris. */
export const EARS = {
  up: ['M30 32l8-22 16 16zM90 32l-8-22-16 16z', 'M36 27l4-10 8 8zM84 27l-4-10-8 8z'],
  down: ['M30 34l2-22 20 14zM90 34l-2-22-20 14z', 'M35 28l1-10 9 7zM85 28l-1-10-9 7z'],
  perked: ['M30 32l6-26 18 20zM90 32l-6-26-18 20z', 'M35 27l3-12 9 10zM85 27l-3-12-9 10z'],
} as const

export type PoseSpec = {
  /** Le nom de la pose pour un lecteur d'écran. */
  label: string
  frame?: Frame
  ears?: keyof typeof EARS
  /** Couché, sans pieds, le corps plus large : il dort. */
  lying?: boolean
  back?: ReactNode
  front: ReactNode
  /** Le geste de la pose, toujours sous `motion-safe:`. */
  motion?: string
}

/** Le contour des accessoires : l'encre de Rico, la même dans les deux thèmes, pour qu'un détail reste lisible sur du blanc. */
const OUTLINE = { strokeWidth: 3, strokeLinejoin: 'round', strokeLinecap: 'round' } as const

const ARMS_DOWN = 'M20 72q-14 8-2 22M100 72q14 8 2 22'
const SMILE = 'M50 78q10 9 20 0'
const HAPPY_EYES = 'M35 58q10-11 20 0M65 58q10-11 20 0'

/** Les yeux de face, un peu vers le bas : ceux de la pose neutre, du salut et de la couronne. */
const eyesAhead = (
  <>
    <Eye x={45} look={[47, 58]} />
    <Eye x={75} look={[77, 58]} />
  </>
)

export const POSES = {
  neutral: {
    label: 'Rico est content',
    back: <Arm d={ARMS_DOWN} />,
    front: (
      <>
        <Cheeks y={73} />
        {eyesAhead}
        <Line d={SMILE} />
      </>
    ),
  },
  waving: {
    label: 'Rico salue',
    back: (
      <>
        <Arm d="M20 72q-12 6-10 20" />
        <Arm d="M100 70q16-4 14-26" />
        <Hand x={114} y={42} />
      </>
    ),
    front: (
      <>
        <Cheeks y={73} />
        {eyesAhead}
        <Grin d="M49 76q11 14 22 0z" />
      </>
    ),
  },
  cheering: {
    label: 'Rico encourage',
    motion: 'motion-safe:animate-hop',
    back: (
      <>
        <Arm d="M20 66q-14-6-12-24M100 66q14-6 12-24" />
        <Hand x={8} y={40} />
        <Hand x={112} y={40} />
      </>
    ),
    front: (
      <>
        <Cheeks y={73} />
        <Eye x={45} look={[46, 55]} />
        <Eye x={75} look={[76, 55]} />
        <Grin d="M47 75q13 16 26 0z" />
      </>
    ),
  },
  thinking: {
    label: 'Rico réfléchit',
    motion: 'motion-safe:animate-tilt',
    back: <Arm d="M20 72q-12 6-10 20" />,
    front: (
      <>
        <Cheek x={30} y={73} />
        <Eye x={45} look={[48, 51]} />
        <Eye x={75} look={[78, 51]} />
        <Brow d="M66 38l18-5" />
        <Line d="M50 80h13" />
        {/* La main au menton, et deux bulles de pensée. */}
        <Arm d="M104 96q4-14-22-12" />
        <Hand x={80} y={84} />
        <circle cx="108" cy="20" r="5" className="fill-info-line" />
        <circle cx="120" cy="8" r="7" className="fill-info-line" />
      </>
    ),
  },
  applauding: {
    label: 'Rico applaudit',
    motion: 'motion-safe:animate-hop',
    front: (
      <>
        <Cheeks y={70} />
        <Line d={HAPPY_EYES} width={4.5} />
        <Grin d="M50 68q10 12 20 0z" />
        <Arm d="M20 80q18 16 34 8" />
        <Hand x={55} y={87} />
        <Arm d="M100 80q-18 16-34 8" />
        <Hand x={65} y={87} />
        <path
          d="M60 71v-6M50 74l-4-4M70 74l4-4"
          strokeWidth="3"
          strokeLinecap="round"
          className="stroke-sun"
        />
      </>
    ),
  },
  comforting: {
    label: 'Rico compatit',
    ears: 'down',
    back: <Arm d="M20 74q-10 10-6 22M100 74q10 10 6 22" />,
    front: (
      <>
        <Eye x={45} y={58} look={[46, 62]} />
        <Eye x={75} y={58} look={[74, 62]} />
        <Brow d="M34 42l16 6M86 42l-16 6" />
        <Line d="M51 84q9-8 18 0" />
      </>
    ),
  },
  sleeping: {
    label: 'Rico dort',
    ears: 'down',
    lying: true,
    front: (
      <>
        <Cheek x={28} y={80} />
        <Cheek x={92} y={80} />
        <Line d="M35 64q10 8 20 0M65 64q10 8 20 0" width={4.5} />
        <circle cx="60" cy="84" r="4.5" className="fill-white" />
        <Arm d="M24 98q10 8 22 4M96 98q-10 8-22 4" />
        <path
          d="M98 22h10l-10 10h10M112 6h7l-7 7h7"
          strokeWidth="3.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="fill-none stroke-brand-strong"
        />
      </>
    ),
  },
  trophy: {
    label: 'Rico brandit une coupe',
    frame: [-12, -30, 144, 152],
    motion: 'motion-safe:animate-hop',
    back: <Arm d="M22 48Q12 18 44 6M98 48Q108 18 76 6" />,
    front: (
      <>
        <Cheeks y={72} />
        <Line d={HAPPY_EYES} width={4.5} />
        <Grin d="M48 70q12 16 24 0z" />
        <path
          d="M44 -24h32v12a16 16 0 0 1-32 0z"
          {...OUTLINE}
          className="fill-white stroke-rico-ink"
        />
        <path
          d="M44 -18h-8v4a8 8 0 0 0 8 8M76 -18h8v4a8 8 0 0 1-8 8M60 4v6M50 12h20"
          {...OUTLINE}
          className="fill-none stroke-rico-ink"
        />
        <Hand x={46} y={6} />
        <Hand x={74} y={6} />
      </>
    ),
  },
  crown: {
    label: 'Rico porte une couronne',
    frame: [-12, -8, 144, 130],
    back: <Arm d={ARMS_DOWN} />,
    front: (
      <>
        <Cheeks y={73} />
        <path
          d="M40 26l-4-24 14 12 10-16 10 16 14-12-4 24z"
          {...OUTLINE}
          className="fill-sun stroke-rico-ink"
        />
        <circle cx="60" cy="16" r="3.5" className="fill-rico" />
        {eyesAhead}
        <Line d={SMILE} />
      </>
    ),
  },
  gift: {
    label: 'Rico offre un cadeau',
    front: (
      <>
        <Cheeks y={66} />
        <Eye x={45} y={50} look={[47, 53]} />
        <Eye x={75} y={50} look={[77, 53]} />
        <Grin d="M51 66q9 9 18 0z" />
        <rect
          x="40"
          y="78"
          width="40"
          height="30"
          rx="6"
          {...OUTLINE}
          className="fill-sun stroke-rico-ink"
        />
        <path d="M60 78v30M40 90h40" {...OUTLINE} className="fill-none stroke-rico-ink" />
        <path
          d="M60 78q-12-14-16-4 2 6 16 4zM60 78q12-14 16-4-2 6-16 4z"
          {...OUTLINE}
          className="fill-white stroke-rico-ink"
        />
        <Arm d="M20 80q6 12 18 12M100 80q-6 12-18 12" />
        <Hand x={38} y={93} />
        <Hand x={82} y={93} />
      </>
    ),
  },
  coding: {
    label: 'Rico code',
    front: (
      <>
        <Eye x={45} y={52} look={[46, 56]} />
        <Eye x={75} y={52} look={[74, 56]} />
        {/* Ses lunettes, puis son portable. */}
        <circle cx="45" cy="52" r="14.5" {...OUTLINE} className="fill-none stroke-rico-ink" />
        <circle cx="75" cy="52" r="14.5" {...OUTLINE} className="fill-none stroke-rico-ink" />
        <Line d="M53 72q7 5 14 0" />
        <rect x="30" y="82" width="60" height="28" rx="5" className="fill-rico-ink" />
        <circle cx="60" cy="96" r="4" className="fill-sun" />
        <Arm d="M20 78q-2 16 10 20M100 78q2 16-10 20" />
        <Hand x={31} y={99} />
        <Hand x={89} y={99} />
      </>
    ),
  },
  timer: {
    label: 'Rico regarde le chrono',
    back: <Arm d="M20 72q-12 6-10 20M100 68q14-8 12-22" />,
    front: (
      <>
        <Eye x={45} look={[49, 54]} wide />
        <Eye x={75} look={[79, 54]} wide />
        <circle cx="60" cy="82" r="5" className="fill-white" />
        {/* Une goutte de sueur, du bleu des bulles de la pose qui réfléchit. */}
        <path d="M24 30q-6 9 0 13q6-4 0-13z" className="fill-info-line" />
        <circle cx="113" cy="34" r="13" {...OUTLINE} className="fill-white stroke-rico-ink" />
        <path
          d="M113 34v-7M113 34l5 3M113 21v-5"
          {...OUTLINE}
          className="fill-none stroke-rico-ink"
        />
      </>
    ),
  },
  streak: {
    label: 'Rico tient une flamme',
    frame: [-12, -4, 144, 126],
    back: (
      <>
        <Arm d="M20 72q-12 6-10 20M100 68q14-8 12-20" />
        <Hand x={112} y={46} />
        <path
          d="M112 0c2 9 11 11 11 22a11 11 0 0 1-22 0c0-4 2-7 4-9 0 4 2 6 4 6 0-7-2-11 3-19z"
          className="fill-flame"
        />
        <path
          d="M112 22c1 3 4 3 4 7a4 4 0 0 1-8 0c0-2 1.5-3 2-4.5 1 1 1.5 0 2-2.5z"
          className="fill-sun"
        />
      </>
    ),
    front: (
      <>
        <Cheeks y={73} />
        <Eye x={45} look={[49, 54]} />
        <Eye x={75} look={[79, 54]} />
        <Line d={SMILE} />
      </>
    ),
  },
  reading: {
    label: 'Rico lit',
    front: (
      <>
        <Cheeks y={68} />
        <Eye x={45} y={52} look={[46, 58]} />
        <Eye x={75} y={52} look={[74, 58]} />
        <Line d="M53 72q7 5 14 0" />
        <path
          d="M32 84q14-6 28 0q14-6 28 0v24q-14-6-28 0q-14-6-28 0z"
          {...OUTLINE}
          className="fill-white stroke-rico-ink"
        />
        <path d="M60 84v24" {...OUTLINE} className="fill-none stroke-rico-ink" />
        <Arm d="M20 78q-2 14 8 18M100 78q2 14-8 18" />
        <Hand x={30} y={97} />
        <Hand x={90} y={97} />
      </>
    ),
  },
  winking: {
    label: 'Rico fait un clin d’œil',
    back: <Arm d="M20 72q-14 8-2 22M100 72q12 6 10 20" />,
    front: (
      <>
        <Cheeks y={73} />
        <Eye x={45} look={[47, 58]} />
        <Line d="M65 58q10-9 20 0" width={4.5} />
        <Line d="M48 76q12 12 24 0" />
        <path d="M100 30l2 5 5 2-5 2-2 5-2-5-5-2 5-2z" className="fill-sun" />
      </>
    ),
  },
  surprised: {
    label: 'Rico est surpris',
    ears: 'perked',
    back: (
      <>
        <Arm d="M20 70q-14 0-18-12M100 70q14 0 18-12" />
        <Hand x={2} y={56} />
        <Hand x={118} y={56} />
      </>
    ),
    front: (
      <>
        <Eye x={45} look={[45, 56]} wide />
        <Eye x={75} look={[75, 56]} wide />
        <Brow d="M36 37q9-6 18 0M66 37q9-6 18 0" />
        <ellipse cx="60" cy="84" rx="6" ry="8" className="fill-white" />
      </>
    ),
  },
  pointing: {
    label: 'Rico montre le chemin',
    frame: [-12, 0, 156, 122],
    back: (
      <>
        <Arm d="M20 72q-14 8-2 22M100 72h22" />
        <Hand x={124} y={72} />
        <path d="M128 56l10 0M133 51l5 5-5 5" {...OUTLINE} className="fill-none stroke-flame" />
      </>
    ),
    front: (
      <>
        <Cheeks y={73} />
        <Eye x={45} look={[50, 56]} />
        <Eye x={75} look={[80, 56]} />
        <Line d={SMILE} />
      </>
    ),
  },
} satisfies Record<string, PoseSpec>

/**
 * Rico qui tend une carte, sur la page publique : le salut, une carte jaune au bout du bras. Ce n'est pas une des
 * seize poses, seulement le dessin de l'accroche.
 */
export const HOLDING_A_CARD: PoseSpec = {
  label: 'Rico tend une carte',
  frame: [-12, -8, 166, 130],
  back: (
    <>
      <Arm d="M20 72q-12 6-10 20" />
      <g transform="rotate(12 122 30)">
        <rect x="102" y="2" width="40" height="54" rx="8" className="fill-sun" />
        <path
          d="M113 20h18M113 30h12"
          strokeWidth="3.5"
          strokeLinecap="round"
          className="stroke-on-sun"
        />
      </g>
      <Arm d="M100 68q14-8 12-20" />
      <Hand x={112} y={46} />
    </>
  ),
  front: (
    <>
      <Cheeks y={73} />
      {eyesAhead}
      <Grin d="M49 76q11 14 22 0z" />
    </>
  ),
}
