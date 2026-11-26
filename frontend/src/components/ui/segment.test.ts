import { describe, expect, it } from 'vitest'
import { segment } from './segment'

describe('the segment (ADR 0077)', () => {
  it('is a tall enough target, outlined when unselected', () => {
    const classes = segment().split(' ')

    expect(classes).toEqual(
      expect.arrayContaining(['min-h-[2.875rem]', 'border-2', 'border-line-strong', 'bg-surface']),
    )
  })

  it.each([
    ['learn', 'border-brand', 'bg-brand-soft'],
    ['practice', 'border-info', 'bg-info-soft'],
    ['play', 'border-ink', 'bg-sun-soft'],
  ] as const)('thickens its outline in the colour of the %s door', (door, border, background) => {
    const classes = segment({ selected: true, door }).split(' ')

    expect(classes).toEqual(
      expect.arrayContaining(['border-[3px]', border, background, 'font-black']),
    )
    expect(classes).not.toContain('border-2')
  })

  it('keeps its text in ink, selected or not', () => {
    expect(segment().split(' ')).toContain('text-ink')
    expect(segment({ selected: true, door: 'play' }).split(' ')).toContain('text-ink')
  })
})
