import { EmptyState } from '../components/states/EmptyState'
import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { ApiError } from '../api/errors'
import { useCreateGroup, useMyGroups } from '../api/groups'
import { button } from '../components/ui/button'

const NAME_MAX_LENGTH = 60

/** Mes groupes de révision, et la création d'un groupe. */
export function GroupsPage() {
  const groups = useMyGroups()

  return (
    <section className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold">Mes groupes</h1>
      {groups.isPending && <LoadingState label="Chargement des groupes…" />}
      {groups.isError && (
        <ErrorState
          message="Impossible de charger tes groupes."
          onRetry={() => void groups.refetch()}
        />
      )}
      {groups.isSuccess && groups.data.length === 0 && (
        <EmptyState
          title="Tu ne fais partie d’aucun groupe."
          text="Un groupe révise les mêmes sujets, suit son classement de la semaine et joue des parties en direct. Crées-en un ci-dessous, ou demande un lien d’invitation."
        />
      )}
      {groups.isSuccess && groups.data.length > 0 && (
        <ul className="grid gap-3 sm:grid-cols-2">
          {groups.data.map((group) => (
            <li key={group.id}>
              <Link
                to={`/groups/${group.id}`}
                className="flex h-full flex-col gap-1 rounded-xl border border-line bg-surface p-4 shadow-sm hover:border-brand-line"
              >
                <span className="font-semibold">{group.name}</span>
                <span className="text-sm text-ink-muted">
                  {group.memberCount} membre{group.memberCount > 1 ? 's' : ''}
                  {group.myRole === 'OWNER' && ' · Tu l’as créé'}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
      <CreateGroupForm />
    </section>
  )
}

function CreateGroupForm() {
  const [name, setName] = useState('')
  const create = useCreateGroup()
  const navigate = useNavigate()

  const submit = (event: FormEvent) => {
    event.preventDefault()
    create.mutate(name, { onSuccess: (group) => navigate(`/groups/${group.id}`) })
  }

  return (
    <form
      onSubmit={submit}
      aria-labelledby="create-group-title"
      className="flex max-w-md flex-col gap-3 rounded-xl bg-surface p-4 shadow-sm"
    >
      <h2 id="create-group-title" className="text-lg font-semibold">
        Créer un groupe
      </h2>
      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Nom du groupe</span>
        <input
          value={name}
          onChange={(event) => setName(event.target.value)}
          maxLength={NAME_MAX_LENGTH}
          required
          className="rounded-lg border border-line-strong px-3 py-2"
        />
      </label>
      <button
        type="submit"
        disabled={name.trim() === '' || create.isPending}
        className={`self-start ${button()}`}
      >
        Créer
      </button>
      {create.isError && (
        <p role="alert">
          {create.error instanceof ApiError
            ? create.error.message
            : "Le groupe n'a pas pu être créé."}
        </p>
      )}
    </form>
  )
}
