import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { Progress } from '../api/progress'
import { Celebration } from './Celebration'
import { DailyGoal } from './DailyGoal'
import { Rico } from './Rico'

const progress = (overrides: Partial<Progress> = {}): Progress => ({
  totalXp: 120,
  streak: 3,
  streakFreezes: 0,
  daysToNextFreeze: 4,
  freezeEarnedToday: false,
  cardsToday: 9,
  dailyGoal: 10,
  goalReachedToday: false,
  subjects: [],
  ...overrides,
})

/** Les confettis à l'écran : des pièces animées, sous un conteneur caché aux lecteurs d'écran. */
const confetti = (container: HTMLElement) =>
  container.querySelectorAll('[aria-hidden="true"] .motion-safe\\:animate-confetti')

describe('the celebrations (ADR 0058)', () => {
  it('throws confetti that no one has to read, click or wait for', () => {
    const { container } = render(<Celebration />)

    const burst = container.firstElementChild
    expect(burst).toHaveAttribute('aria-hidden', 'true')
    // Rien à cliquer, et rien du tout pour qui a demandé moins d'animations.
    expect(burst).toHaveClass('pointer-events-none', 'motion-reduce:hidden')
    expect(confetti(container)).toHaveLength(18)
  })

  it('celebrates the goal of the day when it is reached, not when it already was', () => {
    const { container, rerender } = render(<DailyGoal progress={progress()} />)
    expect(confetti(container)).toHaveLength(0)

    rerender(
      <DailyGoal progress={progress({ cardsToday: 10, goalReachedToday: true, streak: 4 })} />,
    )

    expect(screen.getByRole('status')).toHaveTextContent(
      'Objectif atteint ! Ta série passe à 4 jours.',
    )
    expect(confetti(container)).toHaveLength(18)
  })

  it('does not celebrate again a goal reached before the session', () => {
    const { container } = render(
      <DailyGoal progress={progress({ cardsToday: 12, goalReachedToday: true, streak: 4 })} />,
    )

    expect(screen.getByRole('status')).toHaveTextContent('Objectif atteint !')
    expect(confetti(container)).toHaveLength(0)
  })
})

describe('Rico reacts (ADR 0058)', () => {
  it.each([
    ['cheering', 'motion-safe:animate-hop'],
    ['thinking', 'motion-safe:animate-tilt'],
  ] as const)('moves when %s, only for who accepts motion', (mood, motion) => {
    render(<Rico mood={mood} />)

    expect(screen.getByRole('img')).toHaveClass(motion)
  })

  it('blinks, and stays still otherwise, when happy', () => {
    render(<Rico />)

    const rico = screen.getByRole('img', { name: 'Rico est content' })
    expect(rico).not.toHaveClass('motion-safe:animate-hop')
    expect(rico.querySelector('.motion-safe\\:animate-blink')).not.toBeNull()
  })
})
