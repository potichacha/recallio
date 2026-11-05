import { useId, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/errors'
import {
  usePublicProfileSettings,
  useUpdatePublicProfileSettings,
  type PublicProfileSettings,
} from '../api/publicProfile'
import { button } from './ui/button'

/** Le profil public de compétences (ADR 0043) : privé par défaut, public ou privé de nouveau à tout moment. */
export function PublicProfileForm() {
  const settings = usePublicProfileSettings()
  if (!settings.data) return null
  return <SettingsForm settings={settings.data} />
}

function SettingsForm({ settings }: { settings: PublicProfileSettings }) {
  const handleId = useId()
  const [handle, setHandle] = useState(settings.handle ?? '')
  const [visible, setVisible] = useState(settings.visible)
  const update = useUpdatePublicProfileSettings()

  const submit = (event: FormEvent) => {
    event.preventDefault()
    update.mutate({ handle, visible })
  }

  return (
    <form
      onSubmit={submit}
      aria-labelledby="public-profile-title"
      className="flex max-w-md flex-col gap-4"
    >
      <h2 id="public-profile-title" className="text-lg font-semibold">
        Profil public
      </h2>
      <p className="text-sm text-ink-muted">
        Privé par défaut. Rendu public, il montre à quiconque a son adresse tes sujets, ton niveau,
        ce que le serveur a vérifié et la maîtrise que tu déclares en révision.
      </p>
      <div className="flex flex-col gap-1">
        <label htmlFor={handleId} className="text-sm font-medium">
          Adresse du profil
        </label>
        <span className="flex items-center gap-1">
          <span aria-hidden="true" className="text-ink-muted">
            /u/
          </span>
          <input
            id={handleId}
            value={handle}
            onChange={(event) => setHandle(event.target.value.toLowerCase())}
            required
            minLength={3}
            maxLength={30}
            pattern="[a-z0-9][a-z0-9\-]*[a-z0-9]"
            title="Minuscules, chiffres et tirets, sans tiret au début ni à la fin"
            className="w-full rounded-lg border border-line-strong px-3 py-2"
          />
        </span>
      </div>
      <label className="flex items-center gap-2">
        <input
          type="checkbox"
          checked={visible}
          onChange={(event) => setVisible(event.target.checked)}
        />
        <span>Rendre mon profil public</span>
      </label>
      <button type="submit" disabled={update.isPending} className={`self-start ${button()}`}>
        Enregistrer mon profil public
      </button>
      {update.isSuccess && (
        <p role="status">
          {update.data.visible ? 'Ton profil est public.' : 'Ton profil est privé.'}
        </p>
      )}
      {update.isError && (
        <p role="alert">
          {update.error instanceof ApiError
            ? update.error.message
            : "Le profil public n'a pas pu être enregistré."}
        </p>
      )}
      {settings.visible && settings.handle && (
        <Link
          to={`/u/${settings.handle}`}
          className="self-start text-sm font-medium text-brand-strong hover:underline"
        >
          Voir mon profil public
        </Link>
      )}
    </form>
  )
}
