import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterEach } from 'vitest'

// Les pages se chargent à la demande : sur une machine occupée, la seconde par défaut ne suffit pas toujours.
configure({ asyncUtilTimeout: 3000 })

afterEach(() => {
  cleanup()
  // Les réglages retenus sur l'appareil (durée de session, sons, rappel refusé...) ne passent pas d'un test à l'autre.
  localStorage.clear()
})
