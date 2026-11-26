import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Rico, RicoWithCard, type RicoPose } from './Rico'
import { POSES } from './ricoPoses'

const poses = Object.keys(POSES) as RicoPose[]

describe('Rico and his poses (ADR 0077)', () => {
  it('has sixteen poses, and a neutral one', () => {
    expect(poses).toHaveLength(17)
    expect(poses).toContain('neutral')
  })

  it('names every pose differently, in French', () => {
    const labels = poses.map((pose) => POSES[pose].label)

    expect(new Set(labels).size).toBe(poses.length)
    for (const label of labels) expect(label).toMatch(/^Rico /)
  })

  it.each(poses)('draws the pose %s under its accessible name', (pose) => {
    const { container } = render(<Rico pose={pose} />)

    const rico = screen.getByRole('img', { name: POSES[pose].label })
    expect(rico).not.toHaveAttribute('aria-hidden')
    // Le corps n'est dessiné qu'une fois, et la pose apporte au moins son visage.
    expect(container.querySelectorAll('rect.fill-rico')).toHaveLength(1)
    expect(rico.childElementCount).toBeGreaterThan(6)
  })

  it.each(poses)('hides the pose %s from screen readers when decorative', (pose) => {
    const { container } = render(<Rico pose={pose} decorative />)

    expect(screen.queryByRole('img')).toBeNull()
    const rico = container.querySelector('svg')
    expect(rico).toHaveAttribute('aria-hidden', 'true')
    expect(rico).not.toHaveAttribute('aria-label')
  })

  it('is neutral when no pose is asked', () => {
    render(<Rico />)

    expect(screen.getByRole('img', { name: 'Rico est content' })).toBeInTheDocument()
  })

  it('keeps the same scale whatever the pose: a raised cup makes the picture taller, not Rico smaller', () => {
    const { container } = render(
      <>
        <Rico pose="waving" size={144} />
        <Rico pose="trophy" size={144} />
        <Rico pose="pointing" size={144} />
      </>,
    )

    const sizes = Array.from(container.querySelectorAll('svg')).map((rico) => [
      rico.getAttribute('width'),
      rico.getAttribute('height'),
    ])
    expect(sizes).toEqual([
      ['144', '122'],
      ['144', '152'],
      ['156', '122'],
    ])
  })

  it.each(['cheering', 'applauding', 'trophy'] as const)('hops once when %s', (pose) => {
    render(<Rico pose={pose} />)

    expect(screen.getByRole('img')).toHaveClass('motion-safe:animate-hop')
  })

  it('never moves without the motion-safe guard', () => {
    const { container } = render(
      <>
        {poses.map((pose) => (
          <Rico key={pose} pose={pose} />
        ))}
      </>,
    )

    const animated = Array.from(container.querySelectorAll('[class*="animate-"]'))
    expect(animated.length).toBeGreaterThan(0)
    for (const element of animated) {
      expect(element.getAttribute('class')).not.toMatch(/(?<!motion-safe:)animate-/)
    }
  })

  it('lies down without feet when sleeping', () => {
    const { container } = render(<Rico pose="sleeping" />)

    expect(container.querySelectorAll('rect.fill-rico-limb')).toHaveLength(0)
  })

  it('holds a card on the landing page, hidden from screen readers and still', () => {
    const { container } = render(<RicoWithCard />)

    const rico = container.querySelector('svg')
    expect(rico).toHaveAttribute('aria-hidden', 'true')
    expect(rico).toHaveAttribute('width', '190')
    expect(rico).not.toHaveClass('motion-safe:animate-hop')
    expect(container.querySelector('rect.fill-sun')).not.toBeNull()
  })
})
