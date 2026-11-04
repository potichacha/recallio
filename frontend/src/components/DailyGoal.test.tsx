import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { TEST_PROGRESS } from '../test/render'
import { DailyGoal } from './DailyGoal'

const goalReached = { ...TEST_PROGRESS, cardsToday: 10, goalReachedToday: true }

describe('DailyGoal', () => {
  it('announces the streak freeze earned on the seventh day in a row', () => {
    render(<DailyGoal progress={{ ...goalReached, streak: 7, freezeEarnedToday: true }} />)

    expect(screen.getByRole('status')).toHaveTextContent(
      'Objectif atteint ! Votre série passe à 7 jours. Elle vous rapporte un gel de streak ❄️, qui couvrira un jour manqué.',
    )
  })

  it('mentions no freeze on the other days', () => {
    render(<DailyGoal progress={{ ...goalReached, streak: 4 }} />)

    expect(screen.getByRole('status')).toHaveTextContent(
      /^Objectif atteint ! Votre série passe à 4 jours\.$/,
    )
  })
})
