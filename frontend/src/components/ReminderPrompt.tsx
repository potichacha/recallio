import { Bell } from '@phosphor-icons/react'
import { useState } from 'react'
import { remindersSupported, useEnableReminders } from '../api/reminders'
import { button } from './ui/button'

const DISMISSED_KEY = 'recallio.reminder-prompt-dismissed-at'
/** Après un « Plus tard », la demande ne revient pas avant deux semaines. */
const QUIET_DAYS = 14
const DAY_MS = 24 * 60 * 60 * 1000
const REMINDER_HOUR = 19

function dismissedRecently(): boolean {
  try {
    const at = Number(localStorage.getItem(DISMISSED_KEY))
    return at > 0 && Date.now() - at < QUIET_DAYS * DAY_MS
  } catch {
    return false
  }
}

function rememberDismissal() {
  try {
    localStorage.setItem(DISMISSED_KEY, String(Date.now()))
  } catch {
    // Stockage indisponible : la demande reviendra à la prochaine session.
  }
}

/**
 * La demande de rappel, au bon moment (ADR 0050) : après une session, quand l'intérêt est évident, jamais à l'arrivée.
 * D'abord notre question, et la fenêtre du navigateur seulement si la réponse est oui : un refus du navigateur est
 * définitif, le nôtre ne l'est pas.
 */
export function ReminderPrompt() {
  const enable = useEnableReminders()
  const [dismissed, setDismissed] = useState(dismissedRecently)
  const undecided = remindersSupported() && Notification.permission === 'default'

  if (enable.isSuccess) {
    return (
      <p role="status" className="text-sm text-success">
        C’est noté : je te rappelle demain à {REMINDER_HOUR} h, si tu n’as pas encore révisé.
      </p>
    )
  }
  if (!undecided || dismissed) return null
  return (
    <section
      aria-label="Rappel quotidien"
      className="flex flex-col gap-3 rounded-xl bg-brand-soft p-4"
    >
      <div className="flex gap-3">
        <Bell weight="bold" aria-hidden className="mt-0.5 size-6 shrink-0 text-brand" />
        <div className="flex flex-col gap-1">
          <p className="font-semibold">Je te rappelle demain à {REMINDER_HOUR} h ?</p>
          <p className="text-sm text-ink-soft">
            Un rappel par jour au plus, seulement si tu n’as pas encore révisé. L’heure se change
            dans ton profil.
          </p>
        </div>
      </div>
      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => enable.mutate(REMINDER_HOUR)}
          disabled={enable.isPending}
          className={button()}
        >
          Oui, rappelle-moi
        </button>
        <button
          type="button"
          onClick={() => {
            rememberDismissal()
            setDismissed(true)
          }}
          className={button({ variant: 'ghost' })}
        >
          Plus tard
        </button>
      </div>
      {enable.isError && (
        <p role="alert" className="text-sm text-danger">
          {enable.error.message || 'Le rappel n’a pas pu être activé.'}
        </p>
      )}
    </section>
  )
}
