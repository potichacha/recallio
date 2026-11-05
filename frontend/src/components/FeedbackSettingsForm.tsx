import { useState } from 'react'
import {
  loadFeedbackSettings,
  saveFeedbackSettings,
  type FeedbackSettings,
} from '../review/feedback'

/** Les retours sensoriels des réponses (ADR 0050), coupés par défaut : on les allume ici, pour cet appareil. */
export function FeedbackSettingsForm() {
  const [settings, setSettings] = useState<FeedbackSettings>(loadFeedbackSettings)
  const toggle = (key: keyof FeedbackSettings) => {
    const next = { ...settings, [key]: !settings[key] }
    saveFeedbackSettings(next)
    setSettings(next)
  }

  return (
    <fieldset className="flex max-w-md flex-col gap-2">
      <legend className="mb-1 text-lg font-semibold">Sons et vibrations</legend>
      <p className="text-sm text-ink-muted">
        Un retour à chaque réponse, sur cet appareil : deux notes si tu savais, une note grave
        sinon.
      </p>
      <label className="flex min-h-11 items-center gap-3">
        <input
          type="checkbox"
          checked={settings.sound}
          onChange={() => toggle('sound')}
          className="size-5 accent-brand"
        />
        Jouer un son
      </label>
      <label className="flex min-h-11 items-center gap-3">
        <input
          type="checkbox"
          checked={settings.vibration}
          onChange={() => toggle('vibration')}
          className="size-5 accent-brand"
        />
        Vibrer (sur les téléphones qui le permettent)
      </label>
    </fieldset>
  )
}
