import { Check } from '@phosphor-icons/react'
import { Link } from 'react-router'
import type { QuestBoard, QuestProgress } from '../api/quests'
import { QUEST_LABELS } from '../components/questLabels'
import { card } from '../components/ui/card'
import { SegmentedProgress } from '../components/ui/SegmentedProgress'
import { heading } from '../components/ui/typography'

// Une date sans heure : lue en UTC pour ne pas glisser d'un jour selon le fuseau du navigateur.
const WEEKDAY = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', timeZone: 'UTC' })

/** Au-delà, une quête (« Réviser 15 cartes ») se lit mieux sur une barre continue que sur des segments. */
const MAX_SEGMENTS = 10

/**
 * Les quêtes sur l'accueil (ADR 0077), en une carte compacte : la première quête du jour qui reste à faire, avec sa
 * progression et l'XP qu'elle rapporte. Toutes les quêtes, du jour et de la semaine, sont sur « Ta progression ».
 */
export function QuestCard({ board, className = '' }: { board: QuestBoard; className?: string }) {
  const quest = board.daily.find((candidate) => !candidate.completed)
  const label = quest ? QUEST_LABELS[quest.quest].title(quest.target) : undefined
  return (
    <section
      aria-labelledby="quest-title"
      className={`flex flex-col gap-2.5 md:rounded-3xl ${card()} ${className}`}
    >
      <div className="flex items-baseline justify-between gap-3">
        <h2 id="quest-title" className={heading({ size: 'md' })}>
          Quête du jour
        </h2>
        {quest && <p className="shrink-0 text-sm font-black text-brand-strong">+{quest.xp} XP</p>}
      </div>
      {quest && label ? (
        <>
          <p className="text-[0.9375rem] leading-snug font-bold text-ink-soft">{label}</p>
          <div className="flex items-center gap-2.5">
            {quest.target <= MAX_SEGMENTS ? (
              <SegmentedProgress
                total={quest.target}
                filled={quest.progress}
                label={`${label} : ${quest.progress} / ${quest.target}`}
              />
            ) : (
              <progress
                value={quest.progress}
                max={quest.target}
                aria-label={`${label} : ${quest.progress} / ${quest.target}`}
                className="h-3 w-full overflow-hidden rounded-full [&::-moz-progress-bar]:rounded-full [&::-moz-progress-bar]:bg-brand [&::-webkit-progress-bar]:bg-surface-sunken [&::-webkit-progress-value]:rounded-full [&::-webkit-progress-value]:bg-brand"
              />
            )}
            <span aria-hidden="true" className="shrink-0 text-[0.9375rem] font-black">
              {quest.progress} / {quest.target}
            </span>
          </div>
        </>
      ) : (
        <p className="text-[0.9375rem] leading-snug font-bold text-ink-soft">
          Tes quêtes du jour sont bouclées. À demain pour les suivantes !
        </p>
      )}
      <Link
        to="/progress"
        className="self-start py-1 text-sm font-black text-brand-strong underline"
      >
        Toutes tes quêtes
      </Link>
    </section>
  )
}

/**
 * Les quêtes du jour et de la semaine (ADR 0040) : elles font varier les formats et récompensent le Tour, jamais la
 * vitesse. Chacune rapporte son XP une fois terminée.
 */
export function QuestsPanel({ board }: { board: QuestBoard }) {
  const lastDay = new Date(`${board.weeklyEndsOn}T00:00:00Z`)
  lastDay.setUTCDate(lastDay.getUTCDate() - 1)
  return (
    <section aria-labelledby="quests-title" className="flex flex-col gap-3">
      <h2 id="quests-title" className="text-lg font-black">
        Quêtes
      </h2>
      {board.daily.length > 0 && <QuestList title="Aujourd’hui" quests={board.daily} />}
      <QuestList
        title={`Cette semaine, jusqu’à ${WEEKDAY.format(lastDay)}`}
        quests={board.weekly}
      />
    </section>
  )
}

function QuestList({ title, quests }: { title: string; quests: QuestProgress[] }) {
  return (
    <div className="flex flex-col gap-2 rounded-2xl bg-surface p-4">
      <h3 className="text-sm font-extrabold text-ink-muted">{title}</h3>
      <ul aria-label={title} className="flex flex-col gap-3">
        {quests.map((quest) => (
          <QuestItem key={quest.quest} quest={quest} />
        ))}
      </ul>
    </div>
  )
}

function QuestItem({ quest }: { quest: QuestProgress }) {
  const { title, hint } = QUEST_LABELS[quest.quest]
  const label = title(quest.target)
  return (
    <li className="flex flex-col gap-1">
      <div className="flex items-baseline justify-between gap-3 text-sm">
        <span className={quest.completed ? 'text-ink-muted line-through' : 'font-extrabold'}>
          {label}
        </span>
        <span
          className={`shrink-0 text-xs font-black ${quest.completed ? 'text-success' : 'text-brand-strong'}`}
        >
          <span className="inline-flex items-center gap-1">
            {quest.completed && <Check weight="bold" aria-hidden />}+{quest.xp} XP
          </span>
        </span>
      </div>
      {hint && !quest.completed && <p className="text-xs text-ink-muted">{hint}</p>}
      <div className="flex items-center gap-2">
        <progress
          value={quest.progress}
          max={quest.target}
          aria-label={`${label} : ${quest.progress} / ${quest.target}`}
          className="h-1.5 w-full overflow-hidden rounded-full [&::-moz-progress-bar]:bg-success [&::-webkit-progress-bar]:bg-surface-sunken [&::-webkit-progress-value]:bg-success"
        />
        <span className="shrink-0 text-xs font-bold text-ink-muted">
          {quest.progress} / {quest.target}
        </span>
      </div>
    </li>
  )
}
