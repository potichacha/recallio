import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { DoorBanner } from './DoorBanner'

describe('the banner of a door (ADR 0077)', () => {
  it('shows its eyebrow above the title of the screen', () => {
    render(<DoorBanner door="practice" eyebrow="S’entraîner" title="Révision intensive" />)

    expect(screen.getByRole('heading', { level: 1, name: 'Révision intensive' })).toBeVisible()
    expect(screen.getByText('S’entraîner')).toHaveClass('uppercase')
  })

  it.each([
    ['learn', 'bg-brand', 'text-on-brand', 'shadow-door-learn'],
    ['practice', 'bg-info', 'text-on-info', 'shadow-door-practice'],
    ['play', 'bg-sun', 'text-on-sun', 'shadow-door-play'],
  ] as const)('takes the colours of the %s door', (door, background, text, edge) => {
    const { container } = render(<DoorBanner door={door} eyebrow="Porte" title="Titre" />)

    expect(container.firstElementChild).toHaveClass(background, text, edge, 'rounded-b-3xl')
  })

  it('hides its illustration from screen readers', () => {
    render(
      <DoorBanner
        door="play"
        eyebrow="Jouer"
        title="Nouvelle partie"
        illustration={<svg data-testid="drawing" />}
      />,
    )

    expect(screen.getByTestId('drawing').parentElement).toHaveAttribute('aria-hidden', 'true')
  })

  it('has no place for an illustration when there is none', () => {
    const { container } = render(<DoorBanner door="learn" eyebrow="Apprendre" title="Java" />)

    expect(container.querySelector('[aria-hidden]')).toBeNull()
  })

  it('shows what follows the title', () => {
    render(
      <DoorBanner door="practice" eyebrow="S’entraîner" title="Révision intensive">
        <p>Des questions sans fin, mélangées, comme en entretien.</p>
      </DoorBanner>,
    )

    expect(screen.getByText(/Des questions sans fin/)).toBeVisible()
  })
})
