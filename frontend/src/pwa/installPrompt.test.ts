import { describe, expect, it } from 'vitest'
import { countVisit, isReturningVisitor } from './installPrompt'

describe('visits', () => {
  it('make a visitor returning from the second visit only', () => {
    expect(isReturningVisitor()).toBe(false)
    countVisit()
    expect(isReturningVisitor()).toBe(false)
    countVisit()
    expect(isReturningVisitor()).toBe(true)
  })
})
