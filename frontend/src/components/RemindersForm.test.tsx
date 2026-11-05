import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { stubApi } from '../test/render'
import { RemindersForm } from './RemindersForm'

function renderForm() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <RemindersForm />
    </QueryClientProvider>,
  )
}

/** Un navigateur qui sait recevoir des notifications, avec un abonnement déjà là, ou pas. */
function supportPush(existing: PushSubscription | null) {
  const created = {
    endpoint: 'https://fcm.googleapis.com/fcm/send/abc',
    toJSON: () => ({ keys: { p256dh: 'BKey', auth: 'secret' } }),
    unsubscribe: vi.fn().mockResolvedValue(true),
  }
  const pushManager = {
    getSubscription: vi.fn().mockResolvedValue(existing),
    subscribe: vi.fn().mockResolvedValue(created),
  }
  vi.stubGlobal('navigator', {
    ...navigator,
    serviceWorker: { ready: Promise.resolve({ pushManager }) },
  })
  vi.stubGlobal('PushManager', function PushManager() {})
  vi.stubGlobal('Notification', { requestPermission: vi.fn().mockResolvedValue('granted') })
  return { pushManager, created }
}

describe('RemindersForm', () => {
  it('explains when the browser cannot receive notifications', () => {
    renderForm()

    expect(screen.getByText(/Ce navigateur ne reçoit pas de notifications/)).toBeInTheDocument()
  })

  it('subscribes this device at the chosen hour, once the user allows notifications', async () => {
    const { pushManager } = supportPush(null)
    const fetchMock = stubApi({
      '/api/v1/push/public-key': { body: { publicKey: 'BKey' } },
      'PUT /api/v1/me/push-subscription': { status: 204, body: null },
    })
    renderForm()

    await userEvent.selectOptions(await screen.findByLabelText('Heure du rappel'), '8')
    await userEvent.click(screen.getByRole('button', { name: 'Activer les rappels' }))

    expect(
      await screen.findByRole('button', { name: 'Désactiver les rappels' }),
    ).toBeInTheDocument()
    expect(pushManager.subscribe).toHaveBeenCalledWith(
      expect.objectContaining({ userVisibleOnly: true }),
    )
    const put = fetchMock.mock.calls
      .map(([request]) => request as Request)
      .find((request) => request.method === 'PUT')
    expect(await put?.json()).toEqual({
      endpoint: 'https://fcm.googleapis.com/fcm/send/abc',
      p256dh: 'BKey',
      auth: 'secret',
      reminderHour: 8,
    })
  })

  it('unsubscribes this device, in the browser and on the server', async () => {
    const existing = {
      endpoint: 'https://fcm.googleapis.com/fcm/send/old',
      unsubscribe: vi.fn().mockResolvedValue(true),
    } as unknown as PushSubscription
    supportPush(existing)
    const fetchMock = stubApi({
      'DELETE /api/v1/me/push-subscription': { status: 204, body: null },
    })
    renderForm()

    await userEvent.click(await screen.findByRole('button', { name: 'Désactiver les rappels' }))

    expect(await screen.findByRole('button', { name: 'Activer les rappels' })).toBeInTheDocument()
    expect(existing.unsubscribe).toHaveBeenCalled()
    const removal = fetchMock.mock.calls
      .map(([request]) => request as Request)
      .find((request) => request.method === 'DELETE')
    expect(new URL(removal?.url ?? '').searchParams.get('endpoint')).toBe(existing.endpoint)
  })
})
