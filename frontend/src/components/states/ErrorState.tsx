import { Rico } from '../Rico'
import { button } from '../ui/button'

/**
 * Un écran ou un bloc qui n'a pas pu charger (ADR 0058) : ce qui n'a pas marché, et de quoi réessayer sans recharger
 * la page ni perdre ce qui est déjà affiché. Pour une panne, pas pour une erreur métier (« Sujet introuvable »), qui
 * garde son message et son lien de retour.
 */
export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div
      role="alert"
      className="flex items-center gap-3 rounded-2xl border border-danger-line bg-danger-soft p-4"
    >
      <Rico mood="thinking" size={48} decorative />
      <div className="flex flex-1 flex-col items-start gap-2">
        <p className="font-semibold">{message}</p>
        {onRetry && (
          <button
            type="button"
            onClick={onRetry}
            className={button({ variant: 'secondary', size: 'sm' })}
          >
            Réessayer
          </button>
        )}
      </div>
    </div>
  )
}
