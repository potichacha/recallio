import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ChoiceOption } from './ChoiceOption'
import type { ChoiceOutcome } from './choices'

function choice(outcome?: ChoiceOutcome, checked = false) {
  render(
    <ChoiceOption
      text="Dans le tas"
      type="radio"
      checked={checked}
      onChange={() => {}}
      outcome={outcome}
    />,
  )
  return screen.getByRole('radio').closest('label') as HTMLLabelElement
}

describe('a choice of a question (ADR 0058)', () => {
  it('is a large target with a visible border, before the answer', () => {
    const label = choice()

    expect(label).toHaveClass('min-h-11', 'border-2', 'border-line-strong')
    expect(label).toHaveTextContent(/^Dans le tas$/)
    expect(label.querySelector('svg')).toBeNull()
    expect(screen.getByRole('radio')).toBeEnabled()
  })

  // Le verdict d'un choix ne repose jamais sur sa seule couleur : il a un texte et une icône.
  it.each([
    ['correct', 'Bonne réponse'],
    ['missed', 'Bonne réponse, non cochée'],
    ['wrong', 'Faux'],
  ] as const)('says in words and with an icon that a choice is %s', (outcome, text) => {
    const label = choice(outcome, outcome !== 'missed')

    expect(label).toHaveTextContent(text)
    expect(label.querySelector('svg')).not.toBeNull()
    expect(screen.getByRole('radio')).toBeDisabled()
  })

  it('leaves a wrong choice that was not picked without a verdict', () => {
    const label = choice('neutral')

    expect(label).toHaveTextContent(/^Dans le tas$/)
    expect(label.querySelector('svg')).toBeNull()
  })
})
