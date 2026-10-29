import { describe, expect, it } from 'vitest'
import { formatDelay } from './formatDelay'

const MINUTE = 60_000
const DAY = 24 * 60 * MINUTE

describe('formatDelay', () => {
  it.each([
    [0, '1 min'],
    [MINUTE, '1 min'],
    [5.5 * MINUTE, '6 min'],
    [10 * MINUTE, '10 min'],
    [3 * 60 * MINUTE, '3 h'],
    [DAY, '1 j'],
    [8 * DAY, '8 j'],
    [46 * DAY, '2 mois'],
    [365 * DAY, '1 an'],
    [3 * 365 * DAY, '3 ans'],
  ])('formats %d ms as %s', (delay, expected) => {
    expect(formatDelay(delay)).toBe(expected)
  })
})
