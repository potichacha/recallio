import { useState, type FormEvent } from 'react'
import { useMistakes } from '../api/mistakes'
import { mistakesPath } from '../practice/mistakesPath'
import { Link } from 'react-router'
import { useCatalogSubjects } from '../api/content'
import { useFollowSubjects, useProfile } from '../api/profile'
import { useReviewStats } from '../api/stats'
import { Chip, Choices } from '../components/ChoiceChips'
import { button } from '../components/ui/button'
import { SubjectLogo } from '../subjects/SubjectLogo'
import {
  DEFAULT_MINUTES,
  DEFAULT_MODE,
  loadSessionSettings,
  saveSessionSettings,
  SESSION_MINUTES,
  type SessionMinutes,
  type SessionMode,
  type SessionSettings,
} from './sessionSettings'

const DURATIONS: SessionMinutes[] = [...SESSION_MINUTES, null]

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

/**
 * Le lancement d'une session sur un sujet, choisi dans la page Révisions (ADR 0048, 0052) : une durée, et toutes les
 * questions ou seulement celles que le serveur corrige. Les derniers réglages sont retenus sur l'appareil. Réviser un
 * sujet le fait suivre : il entre au tableau de bord.
 */
export function SessionSetup({
  subject,
  onStart,
}: {
  subject: string
  onStart: (settings: SessionSettings) => void
}) {
  const profile = useProfile()
  const subjects = useCatalogSubjects()
  const stats = useReviewStats()
  const mistakeCount = useMistakes(subject).data?.cards.length ?? 0
  const follow = useFollowSubjects()
  const [stored] = useState(loadSessionSettings)
  const [minutes, setMinutes] = useState<SessionMinutes>(
    stored.minutes === undefined ? DEFAULT_MINUTES : stored.minutes,
  )
  const [mode, setMode] = useState<SessionMode>(stored.mode ?? DEFAULT_MODE)

  const info = subjects.data?.find((candidate) => candidate.slug === subject)
  const counts = stats.data?.subjects.find((candidate) => candidate.slug === subject)

  const start = (event: FormEvent) => {
    event.preventDefault()
    const followed = profile.data?.followedSubjects
    if (followed && !followed.includes(subject)) follow.mutate([...followed, subject])
    const settings = { subject, minutes, mode }
    saveSessionSettings(settings)
    onStart(settings)
  }

  return (
    <form onSubmit={start} className="flex flex-col gap-6">
      <header className="flex flex-col gap-3">
        <Link to="/review" className="self-start text-sm text-brand hover:underline">
          ← Changer de sujet
        </Link>
        <div className="flex items-center gap-3">
          {info && <SubjectLogo subject={info} />}
          <div className="flex flex-col">
            <h1 className="text-2xl font-bold">Réviser {info?.title ?? subject}</h1>
            {counts && (
              <p className="text-sm text-ink-muted">
                {counts.counts.dueNow} à revoir · {plural(counts.newToday, 'nouvelle')}
              </p>
            )}
          </div>
        </div>
        {mistakeCount > 0 && (
          <Link
            to={mistakesPath(subject)}
            className="self-start text-sm font-medium text-brand-strong hover:underline"
          >
            Revoir mes erreurs de la semaine ({mistakeCount})
          </Link>
        )}
      </header>
      <Choices legend="Durée">
        {DURATIONS.map((value) => (
          <Chip
            key={value ?? 'none'}
            name="minutes"
            checked={minutes === value}
            onChange={() => setMinutes(value)}
          >
            {value === null ? 'Sans limite' : `${value} min`}
          </Chip>
        ))}
      </Choices>
      <Choices legend="Questions">
        <Chip name="mode" checked={mode === 'all'} onChange={() => setMode('all')}>
          <span className="font-medium">Toutes les questions</span>
          <span className="text-xs text-ink-muted">
            Mises en situation et questions à auto-évaluer comprises
          </span>
        </Chip>
        <Chip name="mode" checked={mode === 'graded'} onChange={() => setMode('graded')}>
          <span className="font-medium">Questions corrigées</span>
          <span className="text-xs text-ink-muted">
            QCM, vrai ou faux, code à compléter et exercices
          </span>
        </Chip>
      </Choices>
      <button type="submit" className={`self-start ${button()}`}>
        Commencer
      </button>
    </form>
  )
}
