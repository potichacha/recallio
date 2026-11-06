import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { fakeSession, renderAt, stubApi, TEST_SUBJECTS } from '../test/render'

const HEALTH = '/api/v1/health'
const SUBJECTS = { '/api/v1/subjects': { body: TEST_SUBJECTS } }

describe('HomePage', () => {
  it('shows the API status when the backend is up', async () => {
    stubApi({ ...SUBJECTS, [HEALTH]: { body: { status: 'UP', checks: [] } } })
    renderAt('/')
    expect(await screen.findByText('API : UP')).toBeInTheDocument()
  })

  it('shows DOWN when the backend reports an unhealthy state', async () => {
    stubApi({ ...SUBJECTS, [HEALTH]: { status: 503, body: { status: 'DOWN', checks: [] } } })
    renderAt('/')
    expect(await screen.findByText('API : DOWN')).toBeInTheDocument()
  })

  it('shows unreachable when the request fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => Promise.reject(new TypeError('network'))),
    )
    renderAt('/')
    expect(await screen.findByText('API : injoignable')).toBeInTheDocument()
  })

  it('offers to try every published subject with a Tour, without an account', async () => {
    stubApi({ ...SUBJECTS, [HEALTH]: { body: { status: 'UP', checks: [] } } })
    renderAt('/')

    const trial = await screen.findByRole('region', { name: 'Essaie sans compte' })
    expect(screen.getByRole('link', { name: 'Essayer Java' })).toHaveAttribute('href', '/try/java')
    expect(screen.getByRole('link', { name: 'Essayer Go' })).toHaveAttribute('href', '/try/go')
    expect(trial).not.toHaveTextContent('Rust')
  })

  it('signs in, or shows what there is to learn', async () => {
    stubApi({ ...SUBJECTS, [HEALTH]: { body: { status: 'UP', checks: [] } } })
    const session = fakeSession()
    renderAt('/', session)

    expect(screen.getByRole('link', { name: 'Voir ce qu’on peut apprendre' })).toHaveAttribute(
      'href',
      '/learn',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Me connecter ou créer un compte' }))
    expect(session.login).toHaveBeenCalledWith('/')
  })
})
