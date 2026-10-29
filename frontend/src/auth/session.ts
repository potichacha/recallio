import { createContext, useContext } from 'react'

/**
 * Ce que l'application sait de la connexion, indépendamment de la bibliothèque OIDC. Les composants n'utilisent que
 * cette interface : les tests fournissent une session factice, et changer de bibliothèque ne touche qu'un fichier.
 */
export type Session = {
  status: 'loading' | 'anonymous' | 'authenticated'
  /** Nom fourni par le fournisseur d'identité (le nom affiché choisi par l'utilisateur vient de /me). */
  userName?: string
  /** Page à rouvrir après la connexion. */
  returnTo?: string
  error?: string
  login: (returnTo?: string) => void
  logout: () => void
}

export const SessionContext = createContext<Session | undefined>(undefined)

export function useSession(): Session {
  const session = useContext(SessionContext)
  if (session === undefined) {
    throw new Error('useSession doit être utilisé sous un fournisseur de session')
  }
  return session
}
