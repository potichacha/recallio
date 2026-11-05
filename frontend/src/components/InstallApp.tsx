import { DeviceMobile } from '@phosphor-icons/react'
import { useState, useSyncExternalStore } from 'react'
import {
  installAvailable,
  iosInstallable,
  promptInstall,
  subscribeToInstall,
} from '../pwa/installPrompt'
import { button } from './ui/button'

/**
 * Installer l'application (ADR 0050) : le bouton du navigateur quand il l'annonce, la marche à suivre sur iPhone, rien
 * sinon. Installée, elle s'ouvre comme une application et reçoit les rappels, même sur iPhone.
 */
export function InstallApp() {
  const available = useSyncExternalStore(subscribeToInstall, installAvailable, () => false)
  const [installed, setInstalled] = useState(false)

  if (installed) {
    return (
      <p role="status" className="text-sm text-success">
        Recallio est installée : tu la retrouves sur ton écran d’accueil.
      </p>
    )
  }
  if (!available && !iosInstallable()) return null
  return (
    <section
      aria-label="Installer l’application"
      className="flex flex-col gap-3 rounded-xl bg-surface-muted p-4"
    >
      <div className="flex gap-3">
        <DeviceMobile weight="bold" aria-hidden className="mt-0.5 size-6 shrink-0 text-brand" />
        <div className="flex flex-col gap-1">
          <p className="font-semibold">Installe Recallio sur ton téléphone</p>
          <p className="text-sm text-ink-soft">
            {available
              ? 'Elle s’ouvre en un geste, comme une application, et peut te rappeler de réviser.'
              : 'Dans Safari, touche Partager, puis « Sur l’écran d’accueil ».'}
          </p>
        </div>
      </div>
      {available && (
        <button
          type="button"
          onClick={() => void promptInstall().then(setInstalled)}
          className={`self-start ${button({ variant: 'secondary' })}`}
        >
          Installer l’application
        </button>
      )}
    </section>
  )
}
