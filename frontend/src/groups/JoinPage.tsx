import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/errors'
import { useAcceptInvitation, useInvitation } from '../api/groups'
import { button } from '../components/ui/button'

/** La page ouverte par un lien d'invitation : voir le groupe, puis le rejoindre. */
export function JoinPage() {
  const { token = '' } = useParams()
  const invitation = useInvitation(token)
  const accept = useAcceptInvitation()
  const navigate = useNavigate()

  if (invitation.error instanceof ApiError && invitation.error.status === 404) {
    return (
      <section className="flex flex-col items-start gap-3">
        <h1 className="text-2xl font-bold">Invitation invalide</h1>
        <p className="text-ink-muted">
          Ce lien n'est plus valide : il a expiré ou a été remplacé. Demandes-en un nouveau au
          créateur du groupe.
        </p>
        <Link to="/groups" className="text-brand hover:underline">
          Mes groupes
        </Link>
      </section>
    )
  }
  if (invitation.isError) {
    return (
      <ErrorState
        message="Impossible de charger l'invitation."
        onRetry={() => void invitation.refetch()}
      />
    )
  }
  if (invitation.isPending) {
    return <LoadingState label="Chargement de l'invitation…" />
  }

  const { groupId, groupName, memberCount, alreadyMember } = invitation.data
  return (
    <section className="flex max-w-md flex-col items-start gap-4">
      <h1 className="text-2xl font-bold">Rejoindre « {groupName} »</h1>
      <p className="text-ink-muted">
        Ce groupe compte {memberCount} membre{memberCount > 1 ? 's' : ''}. En le rejoignant, ses
        membres verront ton nom.
      </p>
      {alreadyMember ? (
        <>
          <p>Tu fais déjà partie de ce groupe.</p>
          <Link to={`/groups/${groupId}`} className="text-brand hover:underline">
            Ouvrir le groupe
          </Link>
        </>
      ) : (
        <button
          type="button"
          onClick={() =>
            accept.mutate(token, {
              onSuccess: (group) => navigate(`/groups/${group.id}`, { replace: true }),
            })
          }
          disabled={accept.isPending}
          className={button()}
        >
          Rejoindre le groupe
        </button>
      )}
      {accept.isError && (
        <p role="alert">
          {accept.error instanceof ApiError
            ? accept.error.message
            : 'Tu n’as pas pu rejoindre le groupe.'}
        </p>
      )}
    </section>
  )
}
