// Les polices du design system (ADR 0049), servies par l'application elle-même : aucun appel à Google Fonts (RGPD).
import '@fontsource-variable/jetbrains-mono'
import '@fontsource-variable/lexend'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import { OidcSessionProvider } from './auth/OidcSessionProvider'
import './index.css'

const queryClient = new QueryClient()
const root = document.getElementById('root')
if (!root) {
  throw new Error('Élément #root introuvable dans index.html')
}

createRoot(root).render(
  <StrictMode>
    <OidcSessionProvider>
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </OidcSessionProvider>
  </StrictMode>,
)
