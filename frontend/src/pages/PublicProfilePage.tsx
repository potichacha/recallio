import { EmptyState } from '../components/states/EmptyState'
import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { Fire, SealCheck } from '@phosphor-icons/react'
import { useParams } from 'react-router'
import { ApiError } from '../api/errors'
import { usePublicProfile, type PublicSubject } from '../api/publicProfile'
import { ACHIEVEMENT_LABELS } from '../components/questLabels'
import type { Tier } from '../api/ranked'
import { RankIcon } from '../components/RankIcon'
import { rankLabel } from '../components/rankLabels'
import { ShareButton } from '../components/ShareButton'

const plural = (count: number, one: string, many: string) => `${count} ${count > 1 ? many : one}`

/**
 * Le profil public de compétences (ADR 0043), lisible sans compte. Le vérifié (corrigé par le serveur) est séparé du
 * déclaré (la maîtrise en révision personnelle, auto-évaluée en partie).
 */
export function PublicProfilePage() {
  const { handle = '' } = useParams()
  const profile = usePublicProfile(handle)

  if (profile.isPending) return <LoadingState label="Chargement du profil…" />
  if (profile.isError) {
    // Un profil introuvable ou privé n'est pas une panne : rien à réessayer.
    return profile.error instanceof ApiError && profile.error.status === 404 ? (
      <p role="alert">Ce profil n’existe pas, ou il est privé.</p>
    ) : (
      <ErrorState
        message="Impossible de charger ce profil."
        onRetry={() => void profile.refetch()}
      />
    )
  }
  const { displayName, streak, subjects, achievements } = profile.data
  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">{displayName}</h1>
        <p className="inline-flex flex-wrap items-center gap-1 text-sm text-ink-muted">
          /u/{profile.data.handle} ·
          <Fire weight="fill" aria-hidden className="text-flame" />
          série de {plural(streak, 'jour', 'jours')}
        </p>
        <ShareButton
          label="Partager ce profil"
          card={{
            kicker: 'Profil de compétences',
            title: displayName,
            detail:
              subjects.length > 0
                ? subjects.map((subject) => `${subject.title} niveau ${subject.level}`).join(' · ')
                : 'Profil public',
            url: window.location.href,
          }}
        />
      </header>
      <p className="text-sm text-ink-muted">
        <strong>Vérifié</strong> : corrigé par le serveur (exercices jugés par leurs tests, réponses
        corrigées, notions du Tour validées). <strong>Déclaré</strong> : la maîtrise en révision
        personnelle, en partie auto-évaluée.
      </p>
      {subjects.length === 0 && <EmptyState title="Aucun sujet suivi pour l’instant." />}
      <ul className="flex flex-col gap-3">
        {subjects.map((subject) => (
          <SubjectSkills key={subject.slug} subject={subject} />
        ))}
      </ul>
      {achievements.length > 0 && (
        <section aria-labelledby="public-achievements-title" className="flex flex-col gap-2">
          <h2 id="public-achievements-title" className="text-lg font-semibold">
            Succès
          </h2>
          <ul className="flex flex-wrap gap-2">
            {achievements.map(({ achievement }) => {
              const { title, description, icon: Icon } = ACHIEVEMENT_LABELS[achievement]
              return (
                <li
                  key={achievement}
                  className="inline-flex items-center gap-1.5 rounded-full bg-surface px-3 py-1 text-sm shadow-sm"
                  title={description}
                >
                  <Icon weight="fill" aria-hidden className="text-warning" /> {title}
                </li>
              )
            })}
          </ul>
        </section>
      )}
    </section>
  )
}

function SubjectSkills({ subject }: { subject: PublicSubject }) {
  const { verified, declared } = subject
  return (
    <li className="flex flex-col gap-3 rounded-xl bg-surface p-4 shadow-sm">
      <div className="flex items-center gap-2">
        <h2 className="text-lg font-semibold">{subject.title}</h2>
        <span className="rounded-full bg-warning-soft px-2 py-0.5 text-xs font-medium text-warning">
          Niveau {subject.level}
        </span>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <dl aria-label={`${subject.title} : vérifié`} className="flex flex-col gap-1 text-sm">
          <dt className="inline-flex items-center gap-1 text-sm font-semibold text-success">
            <SealCheck weight="fill" aria-hidden /> Vérifié
          </dt>
          {subject.rank && (
            <dd>
              Classé : <RankIcon tier={subject.rank.tier as Tier} />{' '}
              {rankLabel({ tier: subject.rank.tier as Tier, division: subject.rank.division })}
            </dd>
          )}
          <dd>{plural(verified.exercisesPassed, 'exercice réussi', 'exercices réussis')}</dd>
          <dd>
            {plural(verified.correctAnswers, 'réponse corrigée juste', 'réponses corrigées justes')}
          </dd>
          <dd>
            {plural(
              verified.tourNotionsValidated,
              'notion du Tour validée',
              'notions du Tour validées',
            )}
          </dd>
        </dl>
        <dl aria-label={`${subject.title} : déclaré`} className="flex flex-col gap-1 text-sm">
          <dt className="text-sm font-semibold text-ink-muted">Déclaré</dt>
          {declared.notions > 0 ? (
            <>
              <dd>
                {declared.acquired} / {declared.notions} notions acquises, dont {declared.mastered}{' '}
                maîtrisée{declared.mastered > 1 ? 's' : ''}
              </dd>
              <dd>Prêt pour l’entretien à {declared.readinessPercent} %</dd>
            </>
          ) : (
            <dd>{plural(subject.xp, 'point d’XP', 'points d’XP')}</dd>
          )}
        </dl>
      </div>
    </li>
  )
}
