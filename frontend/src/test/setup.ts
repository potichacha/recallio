import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(() => {
  cleanup()
  // Les réglages retenus sur l'appareil (durée de session, sons, rappel refusé...) ne passent pas d'un test à l'autre.
  localStorage.clear()
})
