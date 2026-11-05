import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeAll, describe, expect, it, vi } from 'vitest'
import { listenForInstallPrompt } from '../pwa/installPrompt'
import { InstallApp } from './InstallApp'

/** L'annonce de Chrome et d'Edge : l'application est installable. */
function announceInstall(outcome: 'accepted' | 'dismissed') {
  const event = Object.assign(new Event('beforeinstallprompt', { cancelable: true }), {
    prompt: vi.fn().mockResolvedValue(undefined),
    userChoice: Promise.resolve({ outcome }),
  })
  act(() => {
    window.dispatchEvent(event)
  })
  return event
}

const offer = () => screen.queryByRole('region', { name: 'Installer l’application' })

describe('InstallApp', () => {
  beforeAll(() => listenForInstallPrompt())

  it('offers nothing while the browser has not said the application is installable', () => {
    render(<InstallApp />)

    expect(offer()).not.toBeInTheDocument()
  })

  it('opens the window of the browser, then says the application is installed', async () => {
    render(<InstallApp />)
    const event = announceInstall('accepted')
    expect(event.defaultPrevented).toBe(true)

    await userEvent.click(screen.getByRole('button', { name: 'Installer l’application' }))

    expect(event.prompt).toHaveBeenCalled()
    expect(await screen.findByRole('status')).toHaveTextContent(
      'Recallio est installée : tu la retrouves sur ton écran d’accueil.',
    )
  })

  it('explains how to install it from Safari on an iPhone', () => {
    vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue(
      'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15',
    )
    render(<InstallApp />)

    expect(offer()).toHaveTextContent(
      'Dans Safari, touche Partager, puis « Sur l’écran d’accueil ».',
    )
    expect(
      screen.queryByRole('button', { name: 'Installer l’application' }),
    ).not.toBeInTheDocument()
  })
})
