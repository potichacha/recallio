import { describe, expect, it } from 'vitest'
import { freezeSummary } from './streak'

describe('freezeSummary', () => {
  it('tells the freezes in reserve and the days left before the next one', () => {
    expect(freezeSummary({ streakFreezes: 1, daysToNextFreeze: 4 })).toBe(
      '1 gel · prochain dans 4 jours',
    )
    expect(freezeSummary({ streakFreezes: 0, daysToNextFreeze: 1 })).toBe(
      '0 gel · prochain dans 1 jour',
    )
  })

  it('says when the reserve is full', () => {
    expect(freezeSummary({ streakFreezes: 2, daysToNextFreeze: null })).toBe(
      '2 gels · réserve pleine',
    )
  })
})
