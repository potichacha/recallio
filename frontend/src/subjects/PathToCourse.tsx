import { Navigate, useParams } from 'react-router'

/** L'ancienne adresse du parcours d'un sujet, devenu son cours (ADR 0052) : les liens déjà partagés y mènent encore. */
export function PathToCourse() {
  const { slug = '' } = useParams()
  return <Navigate to={`/subjects/${encodeURIComponent(slug)}/course`} replace />
}
