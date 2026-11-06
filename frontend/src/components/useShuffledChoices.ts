import { useState } from 'react'
import { shuffleChoices, type DisplayedChoice } from './choices'

/**
 * Les choix d'une question dans leur ordre d'affichage, tiré une fois pour elle (`id`) : il ne bouge pas pendant qu'on
 * répond, ni quand la correction arrive.
 */
export function useShuffledChoices(id: string, choices: readonly string[]): DisplayedChoice[] {
  const [drawn, setDrawn] = useState(() => ({ id, choices: shuffleChoices(choices) }))
  if (drawn.id !== id || drawn.choices.length !== choices.length) {
    // Une autre question dans le même composant : on tire son ordre pendant le rendu, comme le conseille React.
    const next = { id, choices: shuffleChoices(choices) }
    setDrawn(next)
    return next.choices
  }
  return drawn.choices
}
