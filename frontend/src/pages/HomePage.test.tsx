import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { renderAt, stubApi } from '../test/render'

const HEALTH = '/api/v1/health'

describe('HomePage', () => {
  it('shows the API status when the backend is up', async () => {
    stubApi({ [HEALTH]: { body: { status: 'UP', checks: [] } } })
    renderAt('/')
    expect(await screen.findByText('API : UP')).toBeInTheDocument()
  })

  it('shows DOWN when the backend reports an unhealthy state', async () => {
    stubApi({ [HEALTH]: { status: 503, body: { status: 'DOWN', checks: [] } } })
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

  it('links to the themes', async () => {
    stubApi({ [HEALTH]: { body: { status: 'UP', checks: [] } } })
    renderAt('/')
    expect(screen.getByRole('link', { name: 'Parcourir les thèmes' })).toHaveAttribute(
      'href',
      '/themes',
    )
  })
})
