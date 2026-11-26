import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { SegmentedProgress } from './SegmentedProgress'

function filledSegments() {
  return Array.from(screen.getByRole('img').children).map((segment) =>
    segment.classList.contains('bg-brand'),
  )
}

describe('the segmented progress (ADR 0077)', () => {
  it('is one image, named by its label', () => {
    render(<SegmentedProgress total={8} filled={3} label="Question 3 sur 8" />)

    expect(screen.getByRole('img', { name: 'Question 3 sur 8' })).toBeInTheDocument()
  })

  it('draws every segment and fills the first ones', () => {
    render(<SegmentedProgress total={5} filled={2} label="Question 2 sur 5" />)

    expect(filledSegments()).toEqual([true, true, false, false, false])
  })

  it.each([
    [-2, 0],
    [0, 0],
    [5, 5],
    [9, 5],
  ])('bounds %i filled segments out of five to %i', (filled, expected) => {
    render(<SegmentedProgress total={5} filled={filled} label="Progression" />)

    expect(filledSegments()).toHaveLength(5)
    expect(filledSegments().filter(Boolean)).toHaveLength(expected)
  })

  it('takes the colours of the dominant card of the home', () => {
    render(<SegmentedProgress total={2} filled={1} label="Leçons faites : 1 sur 2" tone="hero" />)

    const [filled, empty] = Array.from(screen.getByRole('img').children)
    expect(filled).toHaveClass('bg-sun')
    expect(empty).toHaveClass('bg-hero-track')
  })
})
