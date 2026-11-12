import type { MasteryCounts } from '../api/learningPath'
import { masterySummary } from './mastery'

/**
 * Les notions par niveau de maîtrise, en une barre : expertes, puis maîtrisées, acquises et découvertes, sur le fond des
 * notions nouvelles. Les comptes sont cumulatifs (une notion experte est aussi maîtrisée) : chaque segment en retire le
 * niveau supérieur.
 */
export function MasteryBar({ counts, label }: { counts: MasteryCounts; label: string }) {
  const segments = [
    { level: 'expert', count: counts.expert, className: 'bg-brand' },
    { level: 'mastered', count: counts.mastered - counts.expert, className: 'bg-success' },
    { level: 'acquired', count: counts.acquired - counts.mastered, className: 'bg-info' },
    { level: 'discovered', count: counts.discovered - counts.acquired, className: 'bg-sun' },
  ]
  return (
    <div
      role="img"
      aria-label={`${label} : ${masterySummary(counts)}`}
      className="flex h-2 w-full overflow-hidden rounded-full bg-surface-sunken"
    >
      {counts.total > 0 &&
        segments
          .filter((segment) => segment.count > 0)
          .map((segment) => (
            <span
              key={segment.level}
              className={`${segment.className} origin-left motion-safe:animate-fill`}
              style={{ width: `${(segment.count / counts.total) * 100}%` }}
            />
          ))}
    </div>
  )
}
