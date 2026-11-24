import { Check, Star } from '@phosphor-icons/react'
import type { QuestBoard, QuestProgress } from '../api/quests'
import { QUEST_LABELS } from '../components/questLabels'

// Une date sans heure : lue en UTC pour ne pas glisser d'un jour selon le fuseau du navigateur.
const WEEKDAY = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', timeZone: 'UTC' })

const lowerFirst = (text: string) => text.charAt(0).toLowerCase() + text.slice(1)

/**
 * La quête du jour, sur le premier écran de l'accueil (ADR 0058) : une seule, celle qui reste à faire, avec sa barre et
 * l'XP qu'elle rapporte. Les autres sont plus bas, dans `QuestsPanel`.
 */
export function QuestOfTheDay({ quest }: { quest: QuestProgress }) {
  const label = QUEST_LABELS[quest.quest].title(quest.target)
  return (
    <section
      aria-label="Quête du jour"
      className="flex items-center gap-3 rounded-2xl bg-surface px-4 py-3.5"
    >
      <span
        aria-hidden="true"
        className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-sun-soft text-warning"
      >
        <Star weight="fill" className="size-6" />
      </span>
      <div className="flex flex-1 flex-col gap-1.5">
        <p className="text-[0.9375rem] leading-snug font-extrabold">
          Quête du jour : {lowerFirst(label)}
        </p>
        <progress
          value={quest.progress}
          max={quest.target}
          aria-label={`${label} : ${quest.progress} / ${quest.target}`}
          className="h-3 w-full overflow-hidden rounded-full border-2 border-line-strong bg-brand-soft [&::-moz
-progress-bar]:rounded-full [&::-moz-progress-bar]:bg-brand [&::-webkit-progress-bar]:bg-brand-soft [&::-webkit-progress-value]:rounded-full [&::-webkit-progress-value]:bg-brand"
        />
      </div>
      <p className="shrink-0 text-sm font-black text-brand-strong">+{quest.xp} XP</p>
    </section>
  )
}

/**
 * Les quêtes du jour et de la semaine (ADR 0040) : elles font varier les formats et récompensent le Tour, jamais la
 * vitesse. Chacune rapporte son XP une fois terminée. La quête déjà mise en avant en tête de l'accueil (`featured`)
 * n'est pas répétée ici.
 */
export function QuestsPanel({ board, featured }: { board: QuestBoard; featured?: QuestProgress }) {
  const lastDay = new Date(`${board.weeklyEndsOn}T00:00:00Z`)
  lastDay.setUTCDate(lastDay.getUTCDate() - 1)
  const daily = board.daily.filter((quest) => quest.quest !== featured?.quest)
  return (
    <section aria-labelledby="quests-title" className="flex flex-col gap-3">
      <h2 id="quests-title" className="text-lg font-black">
        Quêtes
      </h2>
      {daily.length > 0 && <QuestList title="Aujourd’hui" quests={daily} />}
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
