import { Crown, Diamond, Medal, ShieldStar, type Icon } from '@phosphor-icons/react'
import type { Rank, Tier } from '../api/ranked'

/** Les rangs du classé (ADR 0046), du plus bas au plus haut. */
export const TIER_LABELS: Record<Tier, string> = {
  BRONZE: 'Bronze',
  SILVER: 'Argent',
  GOLD: 'Or',
  PLATINUM: 'Platine',
  DIAMOND: 'Diamant',
  MASTER: 'Maître',
}

/** L'insigne de chaque rang (ADR 0049) : une icône et la couleur de son métal ou de sa pierre. */
export const TIER_ICONS: Record<Tier, { icon: Icon; className: string }> = {
  BRONZE: { icon: Medal, className: 'text-tier-bronze' },
  SILVER: { icon: Medal, className: 'text-tier-silver' },
  GOLD: { icon: Medal, className: 'text-tier-gold' },
  PLATINUM: { icon: ShieldStar, className: 'text-tier-platinum' },
  DIAMOND: { icon: Diamond, className: 'text-tier-diamond' },
  MASTER: { icon: Crown, className: 'text-tier-master' },
}

const DIVISIONS = ['', 'I', 'II', 'III', 'IV']

/** « Or II », ou « Maître », qui n'a pas de division. */
export function rankLabel(rank: Rank): string {
  return rank.tier === 'MASTER'
    ? TIER_LABELS.MASTER
    : `${TIER_LABELS[rank.tier]} ${DIVISIONS[rank.division]}`
}
