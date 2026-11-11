import { useId, useState, type FormEvent } from 'react'
import { useAccountExport, useDeleteAccount } from '../api/account'
import type { Profile } from '../api/profile'
import { useSession } from '../auth/session'
import { button } from './ui/button'

/**
 * Les droits de l'utilisateur sur ses données (ADR 0056) : les télécharger, et supprimer son compte. La suppression
 * est définitive : elle demande de retaper son nom, pour qu'un clic de trop ne suffise pas.
 */
export function AccountDataSection({ profile }: { profile: Profile }) {
  const session = useSession()
  const exportData = useAccountExport()
  const deleteAccount = useDeleteAccount()
  const confirmationId = useId()
  const [deleting, setDeleting] = useState(false)
  const [confirmation, setConfirmation] = useState('')
  const confirmed = confirmation.trim() === profile.displayName

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (!confirmed) return
    // Le compte applicatif est effacé ; reste l'identité, chez le fournisseur de connexion.
    deleteAccount.mutate(undefined, { onSuccess: () => session.deleteIdentity() })
  }

  return (
    <section aria-labelledby="account-data-title" className="flex max-w-md flex-col gap-4">
      <h2 id="account-data-title" className="text-lg font-semibold">
        Mes données
      </h2>
      <p className="text-sm text-ink-muted">
        Tu peux récupérer tout ce que Recallio garde sur toi : ton profil, ta progression, ton
        historique de révision, tes groupes et tes duels, dans un fichier JSON.
      </p>
      <button
        type="button"
        onClick={() => exportData.mutate()}
        disabled={exportData.isPending}
        className={`self-start ${button({ variant: 'secondary' })}`}
      >
        Télécharger mes données
      </button>
      {exportData.isError && <p role="alert">Le téléchargement a échoué. Réessaie.</p>}

      {!deleting ? (
        <button
          type="button"
          onClick={() => setDeleting(true)}
          className={`self-start ${button({ variant: 'danger' })}`}
        >
          Supprimer mon compte
        </button>
      ) : (
        <form
          onSubmit={submit}
          className="flex flex-col gap-3 rounded-lg border border-danger-line bg-danger-soft p-4"
        >
          <p className="text-sm">
            <strong>C'est définitif.</strong> Ta progression, ton XP, ta série, ton rang et ton
            profil public sont effacés tout de suite. Les duels, défis et parties joués avec
            d'autres restent, sous le nom « Compte supprimé ». Si tu te reconnectes ensuite, tu
            repars de zéro.
          </p>
          <label htmlFor={confirmationId} className="text-sm font-medium">
            Pour confirmer, écris ton nom : {profile.displayName}
          </label>
          <input
            id={confirmationId}
            value={confirmation}
            onChange={(event) => setConfirmation(event.target.value)}
            autoComplete="off"
            className="w-full rounded-lg border border-line-strong bg-surface px-3 py-2"
          />
          <div className="flex flex-wrap gap-3">
            <button
              type="submit"
              disabled={!confirmed || deleteAccount.isPending}
              className={button({ variant: 'danger' })}
            >
              Supprimer définitivement
            </button>
            <button
              type="button"
              onClick={() => {
                setDeleting(false)
                setConfirmation('')
              }}
              className={button({ variant: 'ghost' })}
            >
              Annuler
            </button>
          </div>
          {deleteAccount.isError && (
            <p role="alert">La suppression a échoué : rien n'a été effacé. Réessaie.</p>
          )}
        </form>
      )}
    </section>
  )
}
