import { useState } from 'react'
import { useSubjects } from '../api/content'
import { useProfile } from '../api/profile'
import { useProgress } from '../api/progress'
import { useQuests } from '../api/quests'
import { useReviewStats } from '../api/stats'
import { useLayoutMode } from '../components/layoutMode'
import { Rico } from '../components/Rico'
import { ErrorState } from '../components/states/ErrorState'
import { loadSessionSettings } from '../review/sessionSettings'
import { DoorTiles } from './DoorTiles'
import { DueReviewsCard } from './DueReviewsCard'
import { LearnCard } from './LearnCard'
import { QuestCard } from './QuestsPanel'
import { ricoWord } from './ricoWord'

/**
 * L'accueil d'un utilisateur connecté (ADR 0077, qui révise celui de l'ADR 0058). Rico salue et dit un mot de la
 * journée ; puis les révisions du jour, s'il y en a ; la carte dominante, qui reprend la leçon du sujet en cours ; les
 * deux autres portes ; et la quête du jour. Sur un grand écran, les révisions et la quête passent dans une colonne à
 * droite. La série et l'XP sont dans l'en-tête, et mènent à « Ta progression », qui garde le reste : l'objectif du
 * jour, le calendrier, la progression par sujet. Un nouveau venu ne voit aucun bloc à zéro.
 */
export function DashboardPage() {
  useLayoutMode({ wide: true })
  const profile = useProfile()
  const progress = useProgress()
  const stats = useReviewStats()
  const quests = useQuests()
  const subjects = useSubjects()
  const [stored] = useState(loadSessionSettings)

  const followed = profile.data?.followedSubjects ?? []
  // Le sujet en cours : celui de la dernière session sur cet appareil, sinon le premier sujet suivi.
  const current = stored.subject && followed.includes(stored.subject) ? stored.subject : followed[0]
  const subject = subjects.data?.find((candidate) => candidate.slug === current)
  const currentStats = stats.data?.subjects.find((candidate) => candidate.slug === current)
  const firstName = profile.data?.displayName.split(' ')[0]
  // Tant qu'aucune carte n'a été révisée, il n'y a rien à montrer d'une progression.
  const newcomer = stats.data?.overall.seen === 0
  const word =
    progress.data && stats.data
      ? ricoWord({
          firstName,
          progress: progress.data,
          newcomer,
          nothingToReview:
            currentStats !== undefined && currentStats.counts.dueNow + currentStats.newToday === 0,
        })
      : undefined
  const due = stats.data?.overall.dueNow ?? 0

  return (
    <section className="flex flex-col gap-3.5 px-1 md:gap-5 md:px-0 xl:grid xl:grid-cols-[minmax(0,1fr)_20rem] xl:items-start">
      <header className="flex items-end gap-1.5 md:gap-4 xl:col-start-1 xl:row-start-1 xl:self-center">
        <Rico pose="waving" decorative />
        <div className="flex-1 pb-2.5">
          <h1 className="text-[2rem] leading-[1.05] font-black tracking-[-0.03125rem] md:text-[2.5rem] md:tracking-[-0.05rem]">
            {word?.greeting ?? (firstName ? `Salut ${firstName}` : 'Salut')}
          </h1>
          {word && (
            <p className="mt-1.5 text-[0.9375rem] leading-[1.35] font-bold text-ink-muted md:text-[1.0625rem]">
              {word.message}
            </p>
          )}
        </div>
      </header>

      {due > 0 && <DueReviewsCard due={due} />}

      <div className="flex flex-col gap-3.5 md:gap-5 xl:col-start-1 xl:row-start-2 xl:flex-row">
        {stats.isError && (
          <ErrorState
            message="Impossible de charger tes statistiques."
            onRetry={() => void stats.refetch()}
          />
        )}
        {/* Le catalogue dit si le sujet a un Tour : la carte l'attend, plutôt que de changer de forme sous les yeux. */}
        {stats.data && subjects.data && current && (
          <LearnCard
            subject={current}
            title={currentStats?.title ?? subject?.title ?? current}
            hasTour={subject?.hasTour ?? false}
            themes={currentStats?.themes ?? []}
            newcomer={newcomer}
          />
        )}
        <DoorTiles />
      </div>

      {quests.data && (
        <QuestCard
          board={quests.data}
          className={`xl:col-start-2 ${due > 0 ? 'xl:row-start-2' : 'xl:row-span-2 xl:row-start-1'}`}
        />
      )}
    </section>
  )
}
