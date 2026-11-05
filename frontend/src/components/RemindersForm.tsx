import { useEffect, useState } from 'react'
import { ApiError } from '../api/errors'
import { remindersSupported, useDisableReminders, useEnableReminders } from '../api/reminders'

const HOURS = Array.from({ length: 24 }, (_, hour) => hour)
const DEFAULT_HOUR = 19

/**
 * Les rappels (ADR 0047) : une notification par jour au plus, à l'heure choisie, si l'objectif du jour n'est pas encore
 * atteint. Rien sans l'accord explicite de l'utilisateur, appareil par appareil.
 */
export function RemindersForm() {
  const supported = remindersSupported()
  const [subscription, setSubscription] = useState<PushSubscription | null>()
  const [hour, setHour] = useState(DEFAULT_HOUR)
  const enable = useEnableReminders()
  const disable = useDisableReminders()

  useEffect(() => {
    if (!supported) return
    let active = true
    void navigator.serviceWorker.ready
      .then((registration) => registration.pushManager.getSubscription())
      .then((current) => {
        if (active) setSubscription(current)
      })
    return () => {
      active = false
    }
  }, [supported])

  const error = enable.error ?? disable.error
  return (
    <section aria-labelledby="reminders-title" className="flex max-w-md flex-col gap-3">
      <h2 id="reminders-title" className="text-lg font-semibold">
        Rappels
      </h2>
      {!supported ? (
        <p className="text-sm text-slate-600">
          Ce navigateur ne reçoit pas de notifications. Sur iPhone, ajoutez d’abord Recallio à
          l’écran d’accueil.
        </p>
      ) : subscription ? (
        <>
          <p className="text-sm text-slate-600">
            Les rappels sont activés sur cet appareil : une notification par jour au plus, si
            l’objectif du jour n’est pas atteint.
          </p>
          <button
            type="button"
            disabled={disable.isPending}
            onClick={() => disable.mutate(subscription, { onSuccess: () => setSubscription(null) })}
            className="self-start rounded-lg border border-slate-300 px-4 py-2 font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
          >
            Désactiver les rappels
          </button>
        </>
      ) : (
        <>
          <p className="text-sm text-slate-600">
            Une notification par jour au plus, à l’heure choisie, si l’objectif du jour n’est pas
            encore atteint.
          </p>
          <label className="flex items-center gap-2">
            <span className="text-sm font-medium">Heure du rappel</span>
            <select
              value={hour}
              onChange={(event) => setHour(Number(event.target.value))}
              className="rounded-lg border border-slate-300 px-3 py-2"
            >
              {HOURS.map((value) => (
                <option key={value} value={value}>
                  {value} h
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            disabled={enable.isPending || subscription === undefined}
            onClick={() => enable.mutate(hour, { onSuccess: setSubscription })}
            className="self-start rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            Activer les rappels
          </button>
        </>
      )}
      {error && (
        <p role="alert" className="text-sm text-rose-700">
          {error instanceof ApiError || error instanceof Error
            ? error.message
            : 'Les rappels n’ont pas pu changer.'}
        </p>
      )}
    </section>
  )
}
