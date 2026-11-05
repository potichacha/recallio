import { useParams } from 'react-router'
import { ApiError } from '../api/errors'
import { usePublicProfile, type PublicSubject } from '../api/publicProfile'
import { ACHIEVEMENT_LABELS } from '../components/questLabels'
import type { Tier } from '../api/ranked'
import { rankLabel, TIER_ICONS } from '../components/rankLabels'
import { ShareButton } from '../components/ShareButton'

const plural = (count: number, one: string, many: string) => `${count} ${count > 1 ? many : one}`

/**
 * Le profil public de compétences (ADR 0043), lisible sans compte. Le vérifié (corrigé par le serveur) est séparé du
 * déclaré (la maîtrise en révision personnelle, auto-évaluée en partie).
 */
export function PublicProfilePage() {
  const { handle = '' } = useParams()
  const profile = usePublicProfile(handle)

  if (profile.isPending) return <p>Chargement du profil…</p>
  if (profile.isError) {
    return (
      <p role="alert">
        {profile.error instanceof ApiError && profile.error.status === 404
          ? 'Ce profil n’existe pas, ou il est privé.'
          : 'Impossible de charger ce profil.'}
      </p>
    )
  }
  const { displayName, streak, subjects, achievements } = profile.data
  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">{displayName}</h1>
        <p className="text-sm text-slate-500">
          /u/{profile.data.handle} · 🔥 série de {plural(streak, 'jour', 'jours')}
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
      <p className="text-sm text-slate-600">
        <strong>Vérifié</strong> : corrigé par le serveur (exercices jugés par leurs tests, réponses
        corrigées, notions du Tour validées). <strong>Déclaré</strong> : la maîtrise en révision
        personnelle, en partie auto-évaluée.
      </p>
      {subjects.length === 0 && <p>Aucun sujet suivi pour l’instant.</p>}
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
            {achievements.map(({ achievement }) => (
              <li
                key={achievement}
                className="rounded-full bg-white px-3 py-1 text-sm shadow-sm"
                title={ACHIEVEMENT_LABELS[achievement].description}
              >
                <span aria-hidden="true">{ACHIEVEMENT_LABELS[achievement].icon}</span>{' '}
                {ACHIEVEMENT_LABELS[achievement].title}
              </li>
            ))}
          </ul>
        </section>
      )}
    </section>
  )
}

function SubjectSkills({ subject }: { subject: PublicSubject }) {
  const { verified, declared } = subject
  return (
    <li className="flex flex-col gap-3 rounded-xl bg-white p-4 shadow-sm">
      <div className="flex items-center gap-2">
        <h2 className="text-lg font-semibold">{subject.title}</h2>
        <span className="rounded-full bg-orange-50 px-2 py-0.5 text-xs font-medium text-orange-700">
          Niveau {subject.level}
        </span>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <dl aria-label={`${subject.title} : vérifié`} className="flex flex-col gap-1 text-sm">
          <dt className="text-xs font-medium tracking-wide text-emerald-700 uppercase">
            ✓ Vérifié
          </dt>
          {subject.rank && (
            <dd>
              Classé : <span aria-hidden="true">{TIER_ICONS[subject.rank.tier as Tier]}</span>{' '}
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
          <dt className="text-xs font-medium tracking-wide text-slate-500 uppercase">Déclaré</dt>
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
