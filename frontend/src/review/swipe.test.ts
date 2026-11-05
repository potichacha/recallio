import { describe, expect, it } from 'vitest'
import { onControl, SWIPE_THRESHOLD, swipeRating, typingIn } from './swipe'

describe('swipeRating', () => {
  it('rates a card swiped far enough: to the right if known, to the left otherwise', () => {
    expect(swipeRating(SWIPE_THRESHOLD)).toBe('GOOD')
    expect(swipeRating(-SWIPE_THRESHOLD - 40)).toBe('AGAIN')
  })

  it('lets the card come back when the swipe is too short', () => {
    expect(swipeRating(SWIPE_THRESHOLD - 1)).toBeUndefined()
    expect(swipeRating(1 - SWIPE_THRESHOLD)).toBeUndefined()
  })
})

describe('keyboard shortcuts', () => {
  it('leave the keys typed in a field alone', () => {
    expect(typingIn(document.createElement('textarea'))).toBe(true)
    expect(typingIn(document.createElement('button'))).toBe(false)
    expect(typingIn(null)).toBe(false)
  })

  it('leave Space and Enter to the button or the link that has the focus', () => {
    const link = document.createElement('a')
    link.href = '/review'
    expect(onControl(document.createElement('button'))).toBe(true)
    expect(onControl(link)).toBe(true)
    expect(onControl(document.body)).toBe(false)
  })
})
