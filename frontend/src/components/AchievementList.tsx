import { useAchievements } from '../api/quests'
import { ACHIEVEMENT_LABELS } from './questLabels'

const DATE = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long' })

/** Les succès de l'utilisateur (ADR 0040), débloqués ou non : chacun dit ce qui le débloque. */
export function AchievementList() {
  const achievements = useAchievements()
  if (!achievements.data) return null
  const unlocked = achievements.data.filter(({ unlockedAt }) => unlockedAt).length
  return (
    <section aria-labelledby="achievements-title" className="flex max-w-2xl flex-col gap-3">
      <h2 id="achievements-title" className="text-lg font-semibold">
        Mes succès
      </h2>
      <p className="text-sm text-slate-600">
        {unlocked} / {achievements.data.length} débloqués
      </p>
      <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {achievements.data.map(({ achievement, unlockedAt }) => {
          const { title, description, icon } = ACHIEVEMENT_LABELS[achievement]
          return (
            <li
              key={achievement}
              className={`flex items-start gap-3 rounded-xl p-3 shadow-sm ${unlockedAt ? 'bg-white' : 'bg-slate-50'}`}
            >
              <span
                aria-hidden="true"
                className={`text-2xl ${unlockedAt ? '' : 'opacity-40 grayscale'}`}
              >
                {icon}
              </span>
              <div className="flex flex-col">
                <span className={`font-medium ${unlockedAt ? '' : 'text-slate-500'}`}>{title}</span>
                <span className="text-sm text-slate-600">{description}</span>
                <span className="text-xs text-slate-500">
                  {unlockedAt ? `Débloqué le ${DATE.format(new Date(unlockedAt))}` : 'À débloquer'}
                </span>
              </div>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
