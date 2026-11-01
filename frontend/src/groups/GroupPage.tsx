import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/errors'
import {
  useDeleteGroup,
  useGroup,
  useLeaveGroup,
  useRemoveMember,
  type Group,
  type GroupMember,
} from '../api/groups'
import { InvitationPanel } from './InvitationPanel'

/** Un groupe : ses membres ; le créateur les gère et peut supprimer le groupe, un membre peut le quitter. */
export function GroupPage() {
  const { groupId = '' } = useParams()
  const group = useGroup(groupId)

  if (group.error instanceof ApiError && group.error.status === 404) {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-bold">Groupe introuvable</h1>
        <Link to="/groups" className="text-indigo-600 hover:underline">
          Retour à mes groupes
        </Link>
      </section>
    )
  }

  return (
    <section className="flex flex-col gap-4">
      <Link to="/groups" className="text-sm text-indigo-600 hover:underline">
        ← Mes groupes
      </Link>
      <h1 className="text-2xl font-bold">{group.data?.name ?? 'Chargement…'}</h1>
      {group.isError && <p role="alert">Impossible de charger le groupe.</p>}
      {group.data && (
        <>
          <MemberList group={group.data} />
          {group.data.myRole === 'OWNER' ? (
            <>
              <InvitationPanel groupId={group.data.id} />
              <DeleteGroup groupId={group.data.id} />
            </>
          ) : (
            <LeaveGroup groupId={group.data.id} />
          )}
        </>
      )}
    </section>
  )
}

function MemberList({ group }: { group: Group }) {
  const remove = useRemoveMember()
  const isOwner = group.myRole === 'OWNER'

  return (
    <section aria-labelledby="members-title" className="flex flex-col gap-2">
      <h2 id="members-title" className="text-lg font-semibold">
        Membres ({group.members.length})
      </h2>
      <ul className="flex flex-col gap-2">
        {group.members.map((member) => (
          <li
            key={member.userId}
            className="flex items-center justify-between gap-3 rounded-xl bg-white p-3 shadow-sm"
          >
            <span>
              {member.displayName}
              {member.me && <span className="text-slate-500"> (vous)</span>}
              <RoleBadge member={member} />
            </span>
            {isOwner && member.role !== 'OWNER' && (
              <button
                type="button"
                onClick={() => remove.mutate({ groupId: group.id, userId: member.userId })}
                disabled={remove.isPending}
                aria-label={`Retirer ${member.displayName}`}
                className="text-sm text-red-700 hover:underline disabled:opacity-50"
              >
                Retirer
              </button>
            )}
          </li>
        ))}
      </ul>
      {remove.isError && <p role="alert">Le membre n'a pas pu être retiré.</p>}
    </section>
  )
}

function RoleBadge({ member }: { member: GroupMember }) {
  if (member.role !== 'OWNER') return null
  return (
    <span className="ml-2 rounded bg-indigo-50 px-1.5 py-0.5 text-xs font-medium text-indigo-700">
      Créateur
    </span>
  )
}

function LeaveGroup({ groupId }: { groupId: string }) {
  const leave = useLeaveGroup()
  const navigate = useNavigate()

  return (
    <div className="flex flex-col items-start gap-2">
      <button
        type="button"
        onClick={() => leave.mutate(groupId, { onSuccess: () => navigate('/groups') })}
        disabled={leave.isPending}
        className="rounded-lg border border-red-200 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-50"
      >
        Quitter le groupe
      </button>
      {leave.isError && <p role="alert">Vous n'avez pas pu quitter le groupe.</p>}
    </div>
  )
}

/** Suppression en deux temps : un clic de trop ne fait pas disparaître le groupe de tous ses membres. */
function DeleteGroup({ groupId }: { groupId: string }) {
  const [confirming, setConfirming] = useState(false)
  const remove = useDeleteGroup()
  const navigate = useNavigate()

  return (
    <div className="flex flex-col items-start gap-2">
      {confirming ? (
        <div className="flex flex-wrap items-center gap-3">
          <span className="text-sm">Supprimer le groupe pour tous ses membres ?</span>
          <button
            type="button"
            onClick={() => remove.mutate(groupId, { onSuccess: () => navigate('/groups') })}
            disabled={remove.isPending}
            className="rounded-lg bg-red-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
          >
            Confirmer la suppression
          </button>
          <button
            type="button"
            onClick={() => setConfirming(false)}
            className="text-sm text-slate-600 hover:underline"
          >
            Annuler
          </button>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => setConfirming(true)}
          className="rounded-lg border border-red-200 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-50"
        >
          Supprimer le groupe
        </button>
      )}
      {remove.isError && <p role="alert">Le groupe n'a pas pu être supprimé.</p>}
    </div>
  )
}
