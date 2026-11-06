import { describe, expect, it } from 'vitest'
import { shuffleChoices } from './choices'

/** Une source de hasard qui rend ces valeurs, dans l'ordre. */
function randomOf(...values: number[]) {
  let call = 0
  return () => values[call++ % values.length]
}

describe('shuffleChoices', () => {
  it('shows the choices in the drawn order, each with its position among the received ones', () => {
    expect(shuffleChoices(['Compilé', 'Interprété', 'Les deux'], randomOf(0.9, 0.1, 0.5))).toEqual([
      { index: 1, text: 'Interprété' },
      { index: 2, text: 'Les deux' },
      { index: 0, text: 'Compilé' },
    ])
  })

  it('keeps every choice once', () => {
    const shuffled = shuffleChoices(['a', 'b', 'c', 'd', 'e'])

    expect(shuffled.map(({ index }) => index).sort()).toEqual([0, 1, 2, 3, 4])
  })

  it('keeps true or false in its usual order', () => {
    expect(shuffleChoices(['Vrai', 'Faux'], randomOf(0.9, 0.1))).toEqual([
      { index: 0, text: 'Vrai' },
      { index: 1, text: 'Faux' },
    ])
  })
})
