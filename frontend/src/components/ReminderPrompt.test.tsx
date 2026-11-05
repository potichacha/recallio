import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { sentRequests, stubApi } from '../test/render'
import { ReminderPrompt } from './ReminderPrompt'

const DAY_MS = 24 * 60 * 60 * 1000

function renderPrompt() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <ReminderPrompt />
    </QueryClientProvider>,
  )
}

/** Un navigateur qui sait recevoir des notifications, avec la permission donnée. */
function supportPush(permission: NotificationPermission = 'default') {
  const subscription = {
    endpoint: 'https://fcm.googleapis.com/fcm/send/abc',
    toJSON: () => ({ keys: { p256dh: 'BKey', auth: 'secret' } }),
  }
  const pushManager = { subscribe: vi.fn().mockResolvedValue(subscription) }
  vi.stubGlobal('navigator', {
    ...navigator,
    serviceWorker: { ready: Promise.resolve({ pushManager }) },
  })
  vi.stubGlobal('PushManager', function PushManager() {})
  vi.stubGlobal('Notification', {
    permission,
    requestPermission: vi.fn().mockResolvedValue('granted'),
  })
}

const prompt = () => screen.queryByRole('region', { name: 'Rappel quotidien' })

describe('ReminderPrompt', () => {
  it('asks nothing when the browser cannot receive notifications', () => {
    renderPrompt()

    expect(prompt()).not.toBeInTheDocument()
  })

  it('asks nothing once the browser has allowed or refused notifications', () => {
    supportPush('denied')
    renderPrompt()

    expect(prompt()).not.toBeInTheDocument()
  })

  it('asks first, then subscribes this device at 19 h when the answer is yes', async () => {
    supportPush()
    const fetchMock = stubApi({
      '/api/v1/push/public-key': { body: { publicKey: 'BKey' } },
      'PUT /api/v1/me/push-subscription': { status: 204, body: null },
    })
    renderPrompt()

    expect(prompt()).toHaveTextContent('Je te rappelle demain à 19 h ?')
    await userEvent.click(screen.getByRole('button', { name: 'Oui, rappelle-moi' }))

    expect(await screen.findByRole('status')).toHaveTextContent(
      'C’est noté : je te rappelle demain à 19 h, si tu n’as pas encore révisé.',
    )
    const put = sentRequests(fetchMock).find((request) => request.method === 'PUT')
    expect(await put?.json()).toMatchObject({ reminderHour: 19 })
  })

  it('stays quiet for two weeks after « Plus tard »', async () => {
    supportPush()
    const { unmount } = renderPrompt()

    await userEvent.click(screen.getByRole('button', { name: 'Plus tard' }))
    expect(prompt()).not.toBeInTheDocument()
    unmount()
    renderPrompt()
    expect(prompt()).not.toBeInTheDocument()
  })

  it('asks again once the two weeks are over', () => {
    supportPush()
    localStorage.setItem('recallio.reminder-prompt-dismissed-at', String(Date.now() - 15 * DAY_MS))
    renderPrompt()

    expect(prompt()).toBeInTheDocument()
  })
})
