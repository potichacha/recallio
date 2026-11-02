import { useEffect, useState } from 'react'

const TICK_MS = 250

/**
 * Le temps restant, décompté localement à partir de celui donné par le serveur : jamais à partir de l'horloge du
 * téléphone, qui peut être décalée. Pour un nouveau décompte, le composant qui l'utilise est remonté (une `key`).
 */
export function useCountdown(totalMs: number) {
  const [deadline] = useState(() => Date.now() + totalMs)
  const [remaining, setRemaining] = useState(totalMs)
  useEffect(() => {
    const timer = setInterval(() => setRemaining(Math.max(0, deadline - Date.now())), TICK_MS)
    return () => clearInterval(timer)
  }, [deadline])
  return remaining
}
