import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { HomePage } from './HomePage'

function renderWithQueryClient() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <HomePage />
    </QueryClientProvider>,
  )
}

function stubHealthResponse(status: number, body: unknown) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () => Response.json(body, { status })),
  )
}

describe('HomePage', () => {
  it('shows the API status when the backend is up', async () => {
    stubHealthResponse(200, { status: 'UP', checks: [] })
    renderWithQueryClient()
    expect(await screen.findByText('API : UP')).toBeInTheDocument()
  })

  it('shows DOWN when the backend reports an unhealthy state', async () => {
    stubHealthResponse(503, { status: 'DOWN', checks: [] })
    renderWithQueryClient()
    expect(await screen.findByText('API : DOWN')).toBeInTheDocument()
  })

  it('shows unreachable when the request fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => Promise.reject(new TypeError('network'))),
    )
    renderWithQueryClient()
    expect(await screen.findByText('API : injoignable')).toBeInTheDocument()
  })
})
