import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/errors'
import { useFollowSubjects, useProfile, useUpdateProfile, type Profile } from '../api/profile'
import { SubjectPicker } from '../components/SubjectPicker'

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
          className="rounded-lg border border-slate-300 px-3 py-2"
        />
      </label>
      <div className="flex flex-col gap-1">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Fuseau horaire</span>
          <select
            value={timezone}
            onChange={(event) => setTimezone(event.target.value)}
            aria-describedby="timezone-help"
            className="rounded-lg border border-slate-300 px-3 py-2"
          >
            {timezones(profile.timezone).map((zone) => (
              <option key={zone} value={zone}>
                {zone}
              </option>
            ))}
          </select>
        </label>
        <span id="timezone-help" className="text-xs text-slate-500">
          Il détermine à quelle heure commence une nouvelle journée pour votre série (streak).
        </span>
      </div>
      <button
        type="submit"
        disabled={update.isPending}
        className="self-start rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
      >
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
      <p className="text-sm text-slate-600">
        Vos révisions puisent dans ces sujets. Arrêter d'en suivre un ne supprime pas votre
        progression.
      </p>
      <SubjectPicker selected={selected} onChange={setSelected} />
      <button
        type="submit"
        disabled={selected.length === 0 || follow.isPending}
        className="self-start rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
      >
        Enregistrer mes sujets
      </button>
      {follow.isSuccess && <p role="status">Sujets enregistrés.</p>}
      {follow.isError && <p role="alert">Vos sujets n'ont pas pu être enregistrés.</p>}
    </form>
  )
}

export function ProfilePage() {
  const profile = useProfile()

  return (
    <section className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold">Mon profil</h1>
      {profile.isPending && <p>Chargement du profil…</p>}
      {profile.isError && <p role="alert">Impossible de charger le profil.</p>}
      {profile.isSuccess && (
        <>
          {profile.data.roles.includes('admin') && (
            <p className="text-sm text-slate-500">Vous êtes administrateur.</p>
          )}
          <ProfileForm profile={profile.data} />
          <FollowedSubjectsForm profile={profile.data} />
        </>
      )}
    </section>
  )
}
