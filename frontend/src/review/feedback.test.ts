import { describe, expect, it, vi } from 'vitest'
import { answerFeedback, loadFeedbackSettings, saveFeedbackSettings } from './feedback'

describe('answer feedback', () => {
  it('stays silent and still until the user turns it on', () => {
    const vibrate = vi.fn()
    vi.stubGlobal('navigator', { vibrate })

    answerFeedback(true)

    expect(loadFeedbackSettings()).toEqual({ sound: false, vibration: false })
    expect(vibrate).not.toHaveBeenCalled()
  })

  it('vibrates once when the answer was known, three times otherwise', () => {
    const vibrate = vi.fn()
    vi.stubGlobal('navigator', { vibrate })
    saveFeedbackSettings({ sound: false, vibration: true })

    answerFeedback(true)
    answerFeedback(false)

    expect(vibrate.mock.calls).toEqual([[30], [[40, 60, 40]]])
  })

  it('plays two rising notes when the answer was known', () => {
    const oscillators: { frequency: { value: number } }[] = []
    const node = () => ({ connect: (next: unknown) => next })
    vi.stubGlobal(
      'AudioContext',
      class {
        currentTime = 0
        destination = node()
        createOscillator() {
          const oscillator = { ...node(), frequency: { value: 0 }, start: vi.fn(), stop: vi.fn() }
          oscillators.push(oscillator)
          return oscillator
        }
        createGain() {
          return {
            ...node(),
            gain: { setValueAtTime: vi.fn(), exponentialRampToValueAtTime: vi.fn() },
          }
        }
      },
    )
    saveFeedbackSettings({ sound: true, vibration: false })

    answerFeedback(true)

    expect(oscillators.map((oscillator) => oscillator.frequency.value)).toEqual([523, 784])
  })

  it('reads unreadable settings as everything off', () => {
    localStorage.setItem('recallio.feedback', '{oops')

    expect(loadFeedbackSettings()).toEqual({ sound: false, vibration: false })
  })
})
