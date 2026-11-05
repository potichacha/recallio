import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/errors'
import { useSubjects } from '../api/content'
import {
  useChooseTrack,
  useFollowSubjects,
  useProfile,
  useUpdateProfile,
  type LearningTrack,
  type Profile,
} from '../api/profile'
import { useSession } from '../auth/session'
import { AchievementList } from '../components/AchievementList'
import { FeedbackSettingsForm } from '../components/FeedbackSettingsForm'
import { InstallApp } from '../components/InstallApp'
import { PublicProfileForm } from '../components/PublicProfileForm'
import { RemindersForm } from '../components/RemindersForm'
import { SubjectPicker } from '../components/SubjectPicker'
import { TRACKS } from '../components/trackLabels'
import { button } from '../components/ui/button'

function timezones(current: string): string[] {
  const all = Intl.supportedValuesOf('timeZone')
  return all.includes(current) ? all : [current, ...all]
}

function ProfileForm({ profile }: { profile: Profile }) {
  const [displayName, setDisplayName] = useState(profile.displayName)
  const [timezone, setTimezone] = useState(profile.timezone)
  const update = useUpdateProfile()

  const submit = (event: FormEvent) => {
    event.preventDefault()
    update.mutate({ displayName, timezone })
  }

  return (
    <form onSubmit={submit} className="flex max-w-md flex-col gap-4">
      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Nom affiché</span>
        <input
          value={displayName}
          onChange={(event) => setDisplayName(event.target.value)}
          maxLength={50}
          required
          className="rounded-lg border border-line-strong px-3 py-2"
        />
      </label>
      <div className="flex flex-col gap-1">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Fuseau horaire</span>
          <select
            value={timezone}
            onChange={(event) => setTimezone(event.target.value)}
            aria-describedby="timezone-help"
            className="rounded-lg border border-line-strong px-3 py-2"
          >
            {timezones(profile.timezone).map((zone) => (
              <option key={zone} value={zone}>
                {zone}
              </option>
            ))}
          </select>
        </label>
        <span id="timezone-help" className="text-xs text-ink-muted">
          Il détermine à quelle heure commence une nouvelle journée pour ta série (streak).
        </span>
      </div>
      <button type="submit" disabled={update.isPending} className={`self-start ${button()}`}>
        Enregistrer
      </button>
      {update.isSuccess && <p role="status">Profil enregistré.</p>}
      {update.isError && (
        <p role="alert">
          {update.error instanceof ApiError
            ? update.error.message
            : "Le profil n'a pas pu être enregistré."}
        </p>
      )}
    </form>
  )
}

function FollowedSubjectsForm({ profile }: { profile: Profile }) {
  const [selected, setSelected] = useState(profile.followedSubjects)
  const follow = useFollowSubjects()

  const submit = (event: FormEvent) => {
    event.preventDefault()
    follow.mutate(selected)
  }

  return (
    <form
      onSubmit={submit}
      aria-labelledby="followed-subjects-title"
      className="flex max-w-md flex-col gap-4"
    >
      <h2 id="followed-subjects-title" className="text-lg font-semibold">
        Mes sujets
      </h2>
      <p className="text-sm text-ink-muted">
        Tes révisions puisent dans ces sujets. Arrêter d’en suivre un ne supprime pas ta
        progression.
      </p>
      <SubjectPicker selected={selected} onChange={setSelected} />
      <button
        type="submit"
        disabled={selected.length === 0 || follow.isPending}
        className={`self-start ${button()}`}
      >
        Enregistrer mes sujets
      </button>
      {follow.isSuccess && <p role="status">Sujets enregistrés.</p>}
      {follow.isError && <p role="alert">Tes sujets n’ont pas pu être enregistrés.</p>}
    </form>
  )
}

/** Le parcours de chaque sujet suivi (ADR 0032), enregistré dès qu'on le change. */
function TracksForm({ profile }: { profile: Profile }) {
  const subjects = useSubjects()
  const choose = useChooseTrack()
  const title = (slug: string) =>
    subjects.data?.find((subject) => subject.slug === slug)?.title ?? slug

  if (profile.followedSubjects.length === 0) return null
  return (
    <section aria-labelledby="tracks-title" className="flex max-w-md flex-col gap-4">
      <h2 id="tracks-title" className="text-lg font-semibold">
        Mes parcours
      </h2>
      <dl className="flex flex-col gap-1 text-sm text-ink-muted">
        {TRACKS.map(({ track, label, description }) => (
          <div key={track}>
            <dt className="inline font-medium text-ink">{label} :</dt>{' '}
            <dd className="inline">{description}</dd>
          </div>
        ))}
      </dl>
      {profile.followedSubjects.map((slug) => (
        <label key={slug} className="flex items-center justify-between gap-3">
          <span className="font-medium">{title(slug)}</span>
          <select
            value={profile.tracks[slug] ?? 'PREPARE'}
            disabled={choose.isPending}
            onChange={(event) =>
              choose.mutate({ subject: slug, track: event.target.value as LearningTrack })
            }
            className="rounded-lg border border-line-strong px-3 py-2"
          >
            {TRACKS.map(({ track, label }) => (
              <option key={track} value={track}>
                {label}
              </option>
            ))}
          </select>
        </label>
      ))}
      {choose.isSuccess && <p role="status">Parcours enregistré.</p>}
      {choose.isError && (
        <p role="alert">
          {choose.error instanceof ApiError
            ? choose.error.message
            : "Le parcours n'a pas pu être enregistré."}
        </p>
      )}
    </section>
  )
}

export function ProfilePage() {
  const profile = useProfile()
  const session = useSession()

  return (
    <section className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold">Mon profil</h1>
      {profile.isPending && <p>Chargement du profil…</p>}
      {profile.isError && <p role="alert">Impossible de charger le profil.</p>}
      {profile.isSuccess && (
        <>
          {profile.data.roles.includes('admin') && (
            <p className="flex flex-wrap gap-x-3 text-sm text-ink-muted">
              Tu es administrateur :
              <Link to="/admin/reports" className="font-medium text-brand-strong hover:underline">
                Signalements à traiter
              </Link>
              <Link to="/admin/retention" className="font-medium text-brand-strong hover:underline">
                Rétention
              </Link>
            </p>
          )}
          <ProfileForm profile={profile.data} />
          <FollowedSubjectsForm profile={profile.data} />
          <TracksForm profile={profile.data} />
          <PublicProfileForm />
          <RemindersForm />
          <InstallApp />
          <FeedbackSettingsForm />
          <AchievementList />
          <button
            type="button"
            onClick={session.logout}
            className={`self-start ${button({ variant: 'ghost' })}`}
          >
            Se déconnecter
          </button>
        </>
      )}
    </section>
  )
}
