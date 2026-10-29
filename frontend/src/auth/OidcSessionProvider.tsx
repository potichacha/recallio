import { useEffect, useMemo, type ReactNode } from 'react'
import { AuthProvider, useAuth } from 'react-oidc-context'
import { setAccessTokenProvider } from '../api/client'
import { userManager } from './oidc'
import { SessionContext, type Session } from './session'

type LoginState = { returnTo?: string }

function SessionBridge({ children }: { children: ReactNode }) {
  const auth = useAuth()

  useEffect(() => {
    setAccessTokenProvider(async () => {
      const user = await userManager.getUser()
      return user && !user.expired ? user.access_token : undefined
    })
  }, [])

  const session = useMemo<Session>(() => {
    const status = auth.isLoading ? 'loading' : auth.isAuthenticated ? 'authenticated' : 'anonymous'
    const profile = auth.user?.profile
    return {
      status,
      userName: profile?.name ?? profile?.preferred_username,
      returnTo: (auth.user?.state as LoginState | undefined)?.returnTo,
      error: auth.error?.message,
      login: (returnTo) => void auth.signinRedirect({ state: { returnTo } satisfies LoginState }),
      logout: () => void auth.signoutRedirect(),
    }
  }, [auth])

  return <SessionContext.Provider value={session}>{children}</SessionContext.Provider>
}

/** Fournit la session réelle (OIDC) à l'application. */
export function OidcSessionProvider({ children }: { children: ReactNode }) {
  return (
    <AuthProvider userManager={userManager}>
      <SessionBridge>{children}</SessionBridge>
    </AuthProvider>
  )
}
