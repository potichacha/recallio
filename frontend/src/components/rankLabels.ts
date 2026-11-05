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

export const TIER_ICONS: Record<Tier, string> = {
  BRONZE: '🥉',
  SILVER: '🥈',
  GOLD: '🥇',
  PLATINUM: '💠',
  DIAMOND: '💎',
  MASTER: '👑',
}

const DIVISIONS = ['', 'I', 'II', 'III', 'IV']

/** « Or II », ou « Maître », qui n'a pas de division. */
export function rankLabel(rank: Rank): string {
  return rank.tier === 'MASTER'
    ? TIER_LABELS.MASTER
    : `${TIER_LABELS[rank.tier]} ${DIVISIONS[rank.division]}`
}
