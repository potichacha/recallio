import { Check } from '@phosphor-icons/react'
import { Link } from 'react-router'
import type { Readiness } from '../api/stats'

/**
 * L'indice de préparation d'un sujet (ADR 0041), comme le « prêt pour l'examen » d'Ornikar : ses notions essentielles
 * acquises comptent pour moitié, les maîtrisées en entier. Calculé par le serveur, seuil compris.
 */
export function ReadinessGauge({
  subject,
  title,
  readiness,
}: {
  subject: string
  title: string
  readiness: Readiness
}) {
  const { essentials, acquired, mastered, percent, ready } = readiness
  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-baseline justify-between gap-3 text-sm">
        <span className="font-medium">Prêt pour l’entretien</span>
        <span
          className={`inline-flex items-center gap-1 ${ready ? 'font-semibold text-success' : 'text-ink-muted'}`}
        >
          {ready && <Check weight="bold" aria-hidden />}
          {percent} %
        </span>
      </div>
      <progress
        value={percent}
        max={100}
        aria-label={`${title} : prêt pour l’entretien à ${percent} %`}
        className="h-1.5 w-full overflow-hidden rounded-full [&::-moz-progress-bar]:bg-brand [&::-webkit-progress-bar]:bg-surface-sunken [&::-webkit-progress-value]:bg-brand"
      />
      <p className="text-xs text-ink-muted">
        {acquired} / {essentials} notions essentielles acquises, dont {mastered} maîtrisée
        {mastered > 1 ? 's' : ''}
        {ready ? ' : vous êtes prêt.' : ' : prêt à partir de 80 %.'}{' '}
        <Link
          to={`/subjects/${encodeURIComponent(subject)}/exam`}
          className="font-medium text-brand-strong hover:underline"
        >
          Passer un examen blanc
        </Link>
      </p>
    </div>
  )
}
