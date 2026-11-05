import type { Progress } from '../api/progress'

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

/** La réserve de gels de streak (ADR 0036) : « 1 gel · prochain dans 4 jours », ou « … · réserve pleine ». */
export function freezeSummary({
  streakFreezes,
  daysToNextFreeze,
}: Pick<Progress, 'streakFreezes' | 'daysToNextFreeze'>): string {
  const reserve = plural(streakFreezes, 'gel')
  return daysToNextFreeze == null
    ? `${reserve} · réserve pleine`
    : `${reserve} · prochain dans ${plural(daysToNextFreeze, 'jour')}`
}
