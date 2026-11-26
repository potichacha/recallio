import { EARS, HOLDING_A_CARD, POSES, STANDARD_FRAME, type PoseSpec } from './ricoPoses'

export type RicoPose = keyof typeof POSES

/** Le corps debout, et le corps couché de qui dort : plus large, plus bas, sans pieds. */
const STANDING = {
  shadow: 32,
  earsDrop: 0,
  torso: { x: 18, y: 22, width: 84, height: 82, rx: 28 },
  belly: 'M18 76Q60 92 102 76A28 28 0 0 1 74 104H46A28 28 0 0 1 18 76z',
  shine: 'M30 42a18 18 0 0 1 14-11',
}
const LYING = {
  shadow: 36,
  earsDrop: 6,
  torso: { x: 14, y: 32, width: 92, height: 78, rx: 30 },
  belly: 'M14 82Q60 98 106 82A28 28 0 0 1 78 110H42A28 28 0 0 1 14 82z',
  shine: 'M28 52a18 18 0 0 1 14-11',
}

/**
 * Le dessin commun à toutes les poses, dans l'ordre de la planche : l'ombre, les pieds, ce que la pose glisse derrière
 * le corps, les oreilles, le corps, son ventre plus sombre et son reflet, puis le visage et ce que la pose tient
 * devant. L'échelle ne dépend pas de la pose : `size` est la largeur du cadre ordinaire, et un cadre plus grand (une
 * coupe brandie, un bras tendu) agrandit l'image sans rapetisser Rico.
 */
function Drawing({
  spec,
  size,
  decorative,
}: {
  spec: PoseSpec
  size: number
  decorative: boolean
}) {
  const [x, y, width, height] = spec.frame ?? STANDARD_FRAME
  const scale = size / STANDARD_FRAME[2]
  const body = spec.lying ? LYING : STANDING
  const [ears, innerEars] = EARS[spec.ears ?? 'up']
  return (
    <svg
      role={decorative ? undefined : 'img'}
      aria-label={decorative ? undefined : spec.label}
      aria-hidden={decorative || undefined}
      width={Math.round(width * scale)}
      height={Math.round(height * scale)}
      viewBox={`${x} ${y} ${width} ${height}`}
      className={`shrink-0 origin-bottom ${spec.motion ?? ''}`}
    >
      <ellipse cx="60" cy="113" rx={body.shadow} ry="5" opacity="0.12" className="fill-rico-ink" />
      {!spec.lying && (
        <>
          <rect x="37" y="98" width="18" height="13" rx="6.5" className="fill-rico-limb" />
          <rect x="65" y="98" width="18" height="13" rx="6.5" className="fill-rico-limb" />
        </>
      )}
      {spec.back}
      <g transform={body.earsDrop ? `translate(0 ${body.earsDrop})` : undefined}>
        <path d={ears} className="fill-rico" />
        <path d={innerEars} className="fill-rico-ear" />
      </g>
      <rect {...body.torso} className="fill-rico" />
      <path d={body.belly} className="fill-rico-shade" />
      <path
        d={body.shine}
        strokeWidth="5"
        strokeLinecap="round"
        className="fill-none stroke-rico-shine"
      />
      {spec.front}
    </svg>
  )
}

/**
 * Rico, la mascotte (ADR 0058, redessiné par l'ADR 0077) : une carte violette aux oreilles pointues, avec du volume,
 * des bras et des pieds, en seize poses et une pose neutre. Il est dessiné avec les tokens, donc dans les deux thèmes.
 * Il cligne des yeux, sautille quand il encourage, applaudit ou brandit la coupe, penche la tête quand il réfléchit ;
 * pas pour qui a demandé moins d'animations. C'est un dessin de principe : un illustrateur fera mieux les mains, et le
 * remplacer ne touche que ce fichier, `ricoPoses.tsx` et `ricoParts.tsx`.
 *
 * `size` est sa largeur dans le cadre ordinaire, en pixels. `decorative` le cache aux lecteurs d'écran quand le texte
 * voisin dit déjà tout.
 */
export function Rico({
  pose = 'neutral',
  size = 96,
  decorative = false,
}: {
  pose?: RicoPose
  size?: number
  decorative?: boolean
}) {
  return <Drawing spec={POSES[pose]} size={size} decorative={decorative} />
}

/** La largeur du cadre ordinaire qui donne 190 px au dessin de l'accroche, plus large que lui. */
const HERO_SIZE = 165

/**
 * Rico qui tend une carte, sur la page publique : le dessin de l'accroche. Décoratif, le titre voisin dit tout. Il ne
 * sautille pas : aucune action ne l'a provoqué.
 */
export function RicoWithCard() {
  return <Drawing spec={HOLDING_A_CARD} size={HERO_SIZE} decorative />
}
