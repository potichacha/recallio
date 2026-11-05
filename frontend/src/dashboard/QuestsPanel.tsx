import { Check } from '@phosphor-icons/react'
import type { QuestBoard, QuestProgress } from '../api/quests'
import { QUEST_LABELS } from '../components/questLabels'

// Une date sans heure : lue en UTC pour ne pas glisser d'un jour selon le fuseau du navigateur.
const WEEKDAY = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', timeZone: 'UTC' })

/**
 * Les quêtes du jour et de la semaine (ADR 0040) : elles font varier les formats et récompensent le Tour, jamais la
 * vitesse. Chacune rapporte son XP une fois terminée.
 */
export function QuestsPanel({ board }: { board: QuestBoard }) {
  const lastDay = new Date(`${board.weeklyEndsOn}T00:00:00Z`)
  lastDay.setUTCDate(lastDay.getUTCDate() - 1)
  return (
    <section aria-labelledby="quests-title" className="flex flex-col gap-3">
      <h2 id="quests-title" className="text-lg font-semibold">
        Quêtes
      </h2>
      <QuestList title="Aujourd’hui" quests={board.daily} />
      <QuestList
        title={`Cette semaine, jusqu’à ${WEEKDAY.format(lastDay)}`}
        quests={board.weekly}
      />
    </section>
  )
}

function QuestList({ title, quests }: { title: string; quests: QuestProgress[] }) {
  return (
    <div className="flex flex-col gap-2 rounded-xl bg-surface p-4 shadow-sm">
      <h3 className="text-sm font-medium text-ink-muted">{title}</h3>
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
        <span className={quest.completed ? 'text-ink-muted line-through' : 'font-medium'}>
          {label}
        </span>
        <span
          className={`shrink-0 text-xs font-medium ${quest.completed ? 'text-success' : 'text-brand-strong'}`}
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
        <span className="shrink-0 text-xs text-ink-muted">
          {quest.progress} / {quest.target}
        </span>
      </div>
    </li>
  )
}
