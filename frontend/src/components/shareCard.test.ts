import { describe, expect, it } from 'vitest'
import { drawShareCard, shareText, type ShareCard } from './shareCard'

type Call = { name: string; args: unknown[] }

/** Un contexte de dessin qui note chaque appel : jsdom ne dessine pas. */
function recordingContext(): { context: CanvasRenderingContext2D; calls: Call[] } {
  const calls: Call[] = []
  const context = new Proxy(
    {},
    {
      get:
        (_, name) =>
        (...args: unknown[]) =>
          calls.push({ name: String(name), args }),
      set: () => true,
    },
  )
  return { context: context as CanvasRenderingContext2D, calls }
}

const count = (calls: Call[], name: string) => calls.filter((call) => call.name === name).length

const EXPLOIT: ShareCard = {
  kicker: 'Carte des compétences',
  title: 'Chapitre maîtrisé',
  detail: 'Concurrence',
  url: 'https://recallio.example/subjects/go',
}

describe('shareCard (ADR 0045)', () => {
  it('draws the marks of a daily challenge, a filled ring for a success and an empty one for a miss', () => {
    const { context, calls } = recordingContext()

    drawShareCard(context, {
      kicker: 'Défi du jour · Go',
      title: '2/3',
      detail: 'vendredi 20 novembre 2026',
      url: 'https://recallio.example/daily/go',
      marks: [true, false, true],
    })

    expect(count(calls, 'arc')).toBe(3)
    // Un rond plein et une coche par réussite ; un rond vide et une croix pour l'échec.
    expect(count(calls, 'fill')).toBe(2)
    expect(count(calls, 'stroke')).toBe(4)
    expect(calls.filter((call) => call.name === 'fillText').map((call) => call.args[0])).toEqual([
      'Recallio',
      'Défi du jour · Go',
      '2/3',
      'vendredi 20 novembre 2026',
      'recallio.example',
    ])
  })

  it('draws the other cards as before, without marks', () => {
    const { context, calls } = recordingContext()

    drawShareCard(context, EXPLOIT)

    expect(count(calls, 'arc')).toBe(0)
    const title = calls.find((call) => call.name === 'fillText' && call.args[0] === EXPLOIT.title)
    expect(title?.args[3]).toBe(1040)
  })

  it('shares the exploit, or the text the card brings', () => {
    expect(shareText(EXPLOIT)).toBe(
      'Chapitre maîtrisé : Concurrence. Carte des compétences sur Recallio.',
    )
    expect(shareText({ ...EXPLOIT, text: 'À ton tour :' })).toBe('À ton tour :')
  })
})
