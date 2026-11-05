import { useState } from 'react'
import {
  invitationUrl,
  useGroupInvitation,
  useRenewInvitation,
  useRevokeInvitation,
  type GroupInvitation,
} from '../api/groups'
import { button } from '../components/ui/button'

const EXPIRY = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long', timeStyle: 'short' })

/** Le lien d'invitation du groupe, que seul le créateur voit : le créer, le copier, le remplacer, le désactiver. */
export function InvitationPanel({ groupId }: { groupId: string }) {
  const invitation = useGroupInvitation(groupId)
  const renew = useRenewInvitation(groupId)
  const revoke = useRevokeInvitation(groupId)
  const pending = renew.isPending || revoke.isPending

  return (
    <section
      aria-labelledby="invitation-title"
      className="flex flex-col gap-3 rounded-xl bg-surface p-4 shadow-sm"
    >
      <h2 id="invitation-title" className="text-lg font-semibold">
        Inviter des membres
      </h2>
      {invitation.isPending && <p>Chargement du lien…</p>}
      {invitation.isError && <p role="alert">Impossible de charger le lien d'invitation.</p>}
      {invitation.data === null && (
        <>
          <p className="text-sm text-ink-muted">
            Créez un lien et partagez-le : toute personne connectée qui l'ouvre peut rejoindre le
            groupe pendant 7 jours.
          </p>
          <button
            type="button"
            onClick={() => renew.mutate()}
            disabled={pending}
            className={`self-start ${button()}`}
          >
            Créer un lien d'invitation
          </button>
        </>
      )}
      {invitation.data && (
        <>
          <ActiveLink invitation={invitation.data} />
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => renew.mutate()}
              disabled={pending}
              title="L'ancien lien cessera de fonctionner"
              className="rounded-lg border border-line-strong px-3 py-1.5 text-sm font-medium hover:bg-surface-muted disabled:opacity-50"
            >
              Nouveau lien
            </button>
            <button
              type="button"
              onClick={() => revoke.mutate()}
              disabled={pending}
              className="rounded-lg border border-danger-line px-3 py-1.5 text-sm font-medium text-danger hover:bg-danger-soft disabled:opacity-50"
            >
              Désactiver le lien
            </button>
          </div>
        </>
      )}
      {(renew.isError || revoke.isError) && (
        <p role="alert">Le lien d'invitation n'a pas pu être modifié.</p>
      )}
    </section>
  )
}

function ActiveLink({ invitation }: { invitation: GroupInvitation }) {
  const [copied, setCopied] = useState(false)
  const url = invitationUrl(invitation.token)

  // Le presse-papiers n'existe que sur une origine sûre (HTTPS ou localhost) : sinon, on copie le champ à la main.
  const copy = async () => {
    await navigator.clipboard.writeText(url)
    setCopied(true)
  }

  return (
    <div className="flex flex-col gap-2">
      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Lien d'invitation</span>
        <input
          readOnly
          value={url}
          onFocus={(event) => event.target.select()}
          className="rounded-lg border border-line-strong bg-surface-muted px-3 py-2 font-mono text-sm"
        />
      </label>
      <div className="flex flex-wrap items-center gap-3 text-sm">
        {'clipboard' in navigator && (
          <button
            type="button"
            onClick={() => void copy()}
            className="font-medium text-brand hover:underline"
          >
            Copier le lien
          </button>
        )}
        {copied && <span role="status">Lien copié.</span>}
        <span className="text-ink-muted">
          Valable jusqu'au {EXPIRY.format(new Date(invitation.expiresAt))}.
        </span>
      </div>
    </div>
  )
}
