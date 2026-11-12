import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterEach } from 'vitest'

// Les pages se chargent à la demande : sur une machine occupée, charger le code d'une page prend parfois plusieurs
// secondes. Un test qui passe n'attend pas plus longtemps pour autant.
configure({ asyncUtilTimeout: 8000 })

afterEach(() => {
  cleanup()
  // Les réglages retenus sur l'appareil (durée de session, sons, rappel refusé...) ne passent pas d'un test à l'autre.
  localStorage.clear()
})
