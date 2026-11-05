import type { Tier } from '../api/ranked'
import { TIER_ICONS } from './rankLabels'

/** L'insigne d'un rang du classé (ADR 0046, 0049), décoratif : son nom est toujours écrit à côté. */
export function RankIcon({ tier }: { tier: Tier }) {
  const { icon: Icon, className } = TIER_ICONS[tier]
  return <Icon weight="fill" aria-hidden className={`inline ${className}`} />
}
