import { useProfile } from '../api/profile'
import { useSession } from '../auth/session'

/** Vrai pour un admin connecté : il ouvre aussi les sujets en brouillon, pour les relire (CLAUDE.md, section 2.5). */
export function useIsAdmin(): boolean {
  const session = useSession()
  const profile = useProfile(session.status === 'authenticated')
  return profile.data?.roles.includes('admin') ?? false
}
