import { useState, type FormEvent, type ReactNode } from 'react'
import { useSubjects } from '../api/content'
import { useProfile } from '../api/profile'
import { useReviewStats } from '../api/stats'
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
 * Le lancement d'une session (ADR 0048) : un sujet à la fois, une durée, et toutes les questions ou seulement celles que
 * le serveur corrige. Les derniers réglages sont retenus sur l'appareil ; un sujet ouvert depuis le catalogue est
 * proposé, et choisi, même s'il n'est pas suivi.
 */
export function SessionSetup({
  subject: requested,
  onStart,
}: {
  subject?: string
  onStart: (settings: SessionSettings) => void
}) {
  const profile = useProfile()
  const subjects = useSubjects()
  const stats = useReviewStats()
  const [stored] = useState(loadSessionSettings)
  const [picked, setPicked] = useState<string>()
  const [minutes, setMinutes] = useState<SessionMinutes>(
    stored.minutes === undefined ? DEFAULT_MINUTES : stored.minutes,
  )
  const [mode, setMode] = useState<SessionMode>(stored.mode ?? DEFAULT_MODE)

  const followed = profile.data?.followedSubjects ?? []
  const choices = requested && !followed.includes(requested) ? [...followed, requested] : followed
  const subject = picked ?? defaultSubject(choices, requested, stored.subject)
  const titleOf = (slug: string) =>
    subjects.data?.find((candidate) => candidate.slug === slug)?.title ?? slug
  const countsOf = (slug: string) =>
    stats.data?.subjects.find((candidate) => candidate.slug === slug)

  const start = (event: FormEvent) => {
    event.preventDefault()
    if (!subject) return
    const settings = { subject, minutes, mode }
    saveSessionSettings(settings)
    onStart(settings)
  }

  return (
    <form onSubmit={start} className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold">Réviser</h1>
      <Choices legend="Sujet">
        {choices.map((slug) => {
          const counts = countsOf(slug)
          return (
            <Chip
              key={slug}
              name="subject"
              checked={subject === slug}
              onChange={() => setPicked(slug)}
            >
              <span className="font-medium">{titleOf(slug)}</span>
              {counts && (
                <span className="text-xs text-slate-500">
                  {counts.counts.dueNow} à revoir · {plural(counts.newToday, 'nouvelle')}
                </span>
              )}
            </Chip>
          )
        })}
      </Choices>
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
          <span className="text-xs text-slate-500">
            Mises en situation et questions à auto-évaluer comprises
          </span>
        </Chip>
        <Chip name="mode" checked={mode === 'graded'} onChange={() => setMode('graded')}>
          <span className="font-medium">Questions corrigées</span>
          <span className="text-xs text-slate-500">
            QCM, vrai ou faux, code à compléter et exercices
          </span>
        </Chip>
      </Choices>
      <button
        type="submit"
        disabled={!subject}
        className="self-start rounded-lg bg-indigo-600 px-6 py-3 font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
      >
        Commencer
      </button>
    </form>
  )
}

/** Le sujet demandé par le lien, sinon celui de la dernière session s'il est encore suivi, sinon le premier suivi. */
function defaultSubject(choices: string[], requested?: string, stored?: string) {
  if (requested) return requested
  if (stored && choices.includes(stored)) return stored
  return choices.at(0)
}

function Choices({ legend, children }: { legend: string; children: ReactNode }) {
  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-sm font-medium text-slate-700">{legend}</legend>
      <div className="flex flex-wrap gap-2">{children}</div>
    </fieldset>
  )
}

/** Un choix parmi d'autres : un bouton radio, présenté comme une pastille. */
function Chip({
  name,
  checked,
  onChange,
  children,
}: {
  name: string
  checked: boolean
  onChange: () => void
  children: ReactNode
}) {
  return (
    <label className="flex cursor-pointer flex-col rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 hover:border-indigo-300 has-checked:border-indigo-600 has-checked:bg-indigo-50 has-checked:text-indigo-900 has-focus-visible:ring-2 has-focus-visible:ring-indigo-500">
      <input type="radio" name={name} checked={checked} onChange={onChange} className="sr-only" />
      {children}
    </label>
  )
}
