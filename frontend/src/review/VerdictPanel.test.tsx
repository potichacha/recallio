import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createRef } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { VerdictPanel } from './VerdictPanel'

function panel(correct: boolean, onNext = vi.fn()) {
  render(
    <VerdictPanel
      correct={correct}
      title={correct ? 'Bien joué !' : 'Pas tout à fait'}
      detail={correct ? undefined : 'Les bonnes réponses sont marquées.'}
      nextRef={createRef<HTMLButtonElement>()}
      onNext={onNext}
      links={<a href="/lecon">Revoir la leçon</a>}
    >
      Sans ça, une HashMap ne retrouverait pas l’objet.
    </VerdictPanel>,
  )
  const status = screen.getByRole('status')
  return { status, box: status.closest('.sticky') as HTMLElement, onNext }
}

describe('the verdict panel of a review card (ADR 0058)', () => {
  it('shows a right answer in a green panel, with an icon and words, never the colour alone', () => {
    const { status, box } = panel(true)

    expect(status).toHaveTextContent('Bien joué !')
    expect(status.querySelector('svg')).not.toBeNull()
    expect(box).toHaveClass('bg-success-soft')
    expect(box).not.toHaveClass('bg-danger-soft')
    expect(box).toHaveTextContent('Sans ça, une HashMap ne retrouverait pas l’objet.')
    expect(screen.getByRole('button', { name: 'Carte suivante' })).toHaveClass('bg-success')
  })

  it('shows a wrong answer in the colours of the error, and says where the right answers are', () => {
    const { status, box } = panel(false)

    expect(status).toHaveTextContent('Pas tout à fait')
    expect(status.querySelector('svg')).not.toBeNull()
    expect(box).toHaveClass('bg-danger-soft')
    expect(box).toHaveTextContent('Les bonnes réponses sont marquées.')
    expect(screen.getByRole('button', { name: 'Carte suivante' })).toHaveClass('bg-brand')
  })

  it('sticks to the bottom of a phone screen, full width, and stays in the card on a wide screen', () => {
    const { box } = panel(true)

    expect(box).toHaveClass('sticky', 'bottom-0', 'rounded-t-3xl', 'md:static')
    expect(screen.getByRole('button', { name: 'Carte suivante' })).toHaveClass('w-full')
  })

  it('moves on with its only button, and keeps the links of the card under it', async () => {
    const { onNext } = panel(false)

    expect(screen.getByRole('link', { name: 'Revoir la leçon' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Carte suivante' }))
    expect(onNext).toHaveBeenCalledOnce()
  })
})
