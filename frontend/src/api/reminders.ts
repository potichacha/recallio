import { useMutation } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'

/** Les rappels par notification (ADR 0047), sur cet appareil : l'abonnement que le navigateur a créé. */
export type ReminderSubscription = { endpoint: string; p256dh: string; auth: string }

/** S'abonne aux rappels : la clé du serveur, la permission, l'abonnement du navigateur, puis le serveur. */
export function useEnableReminders() {
  return useMutation({
    mutationFn: async (reminderHour: number) => {
      const { data, error, response } = await api.GET('/api/v1/push/public-key')
      if (data === undefined) throw ApiError.from(response, error)
      if ((await Notification.requestPermission()) !== 'granted') {
        throw new Error(
          'Les notifications sont refusées pour ce site : autorisez-les dans le navigateur.',
        )
      }
      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: fromBase64Url(data.publicKey),
      })
      const keys = subscription.toJSON().keys ?? {}
      const saved = await api.PUT('/api/v1/me/push-subscription', {
        body: {
          endpoint: subscription.endpoint,
          p256dh: keys.p256dh ?? '',
          auth: keys.auth ?? '',
          reminderHour,
        },
      })
      if (!saved.response.ok) throw ApiError.from(saved.response, saved.error)
      return subscription
    },
  })
}

/** Retire les rappels de cet appareil, chez le navigateur et sur le serveur. */
export function useDisableReminders() {
  return useMutation({
    mutationFn: async (subscription: PushSubscription) => {
      await subscription.unsubscribe()
      const { error, response } = await api.DELETE('/api/v1/me/push-subscription', {
        params: { query: { endpoint: subscription.endpoint } },
      })
      if (!response.ok) throw ApiError.from(response, error)
    },
  })
}

/** Vrai si ce navigateur sait recevoir des notifications : sur iPhone, une fois l'application sur l'écran d'accueil. */
export function remindersSupported(): boolean {
  return 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window
}

function fromBase64Url(value: string): Uint8Array<ArrayBuffer> {
  const base64 = (value + '='.repeat((4 - (value.length % 4)) % 4))
    .replace(/-/g, '+')
    .replace(/_/g, '/')
  const binary = atob(base64)
  const bytes = new Uint8Array(new ArrayBuffer(binary.length))
  for (let index = 0; index < binary.length; index++) bytes[index] = binary.charCodeAt(index)
  return bytes
}
