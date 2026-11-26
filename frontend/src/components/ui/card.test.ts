import { describe, expect, it } from 'vitest'
import { card } from './card'

describe('the raised card (ADR 0077)', () => {
  it('is a rounded surface with a thick edge underneath', () => {
    const classes = card().split(' ')

    expect(classes).toEqual(expect.arrayContaining(['rounded-2xl', 'bg-surface', 'shadow-relief']))
  })
})
