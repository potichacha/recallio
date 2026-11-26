import { describe, expect, it } from 'vitest'
import { chip } from './chip'

describe('the chip (ADR 0077)', () => {
  it('is a tall enough target, outlined when unselected', () => {
    const classes = chip().split(' ')

    expect(classes).toEqual(
      expect.arrayContaining(['min-h-11', 'rounded-full', 'border-line-strong', 'bg-surface']),
    )
  })

  it.each([
    ['learn', 'bg-brand', 'text-on-brand'],
    ['practice', 'bg-info', 'text-on-info'],
    ['play', 'bg-ink', 'text-surface'],
  ] as const)('takes the colour of the %s door when selected', (door, background, text) => {
    const classes = chip({ selected: true, door }).split(' ')

    expect(classes).toEqual(expect.arrayContaining([background, text, 'font-black']))
    expect(classes).not.toContain('bg-surface')
  })

  it('looks the same on every door until it is selected', () => {
    expect(chip({ door: 'practice' })).toBe(chip())
    expect(chip({ selected: true })).toBe(chip({ selected: true, door: 'learn' }))
  })

  it('stays still when pressed for who asked for less motion', () => {
    expect(chip()).toContain('motion-reduce:active:translate-y-0')
  })
})
