import type { ReactNode } from 'react'
import { Navigate } from 'react-router'
import { useProfile } from '../api/profile'

/**
 * Pour un utilisateur connecté : tant qu'il ne suit aucun sujet, ses révisions seraient vides, il est donc conduit à
 * l'écran de choix des sujets.
 */
export function RequireFollowedSubjects({ children }: { children: ReactNode }) {
  const profile = useProfile()

  if (profile.isPending) {
    return <p>Chargement…</p>
  }
  if (profile.isSuccess && profile.data.followedSubjects.length === 0) {
    return <Navigate to="/welcome" replace />
  }
  return children
}
