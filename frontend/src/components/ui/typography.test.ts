import { describe, expect, it } from 'vitest'
import { eyebrow, heading } from './typography'

describe('the titles of the mockups (ADR 0077)', () => {
  it('writes an eyebrow in spaced capitals', () => {
    expect(eyebrow().split(' ')).toEqual(expect.arrayContaining(['uppercase', 'font-black']))
  })

  it('has two sizes of heading, the screen title by default', () => {
    expect(heading()).toBe(heading({ size: 'lg' }))
    expect(heading({ size: 'lg' })).toContain('text-[1.75rem]')
    expect(heading({ size: 'md' })).toContain('text-[1.1875rem]')
  })

  it('leaves the colour to where the text sits', () => {
    for (const classes of [eyebrow(), heading(), heading({ size: 'md' })]) {
      expect(classes).not.toMatch(/(?:^| )text-(?!\[)/)
    }
  })
})
