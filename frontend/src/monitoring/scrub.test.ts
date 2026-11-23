import type { ErrorEvent as SentryEvent } from '@sentry/browser'
import { describe, expect, it } from 'vitest'
import { scrubBreadcrumb, scrubEvent, scrubText, scrubUrl } from './scrub'

const ORIGIN = 'https://recallio.test'

describe('the addresses sent to Sentry (ADR 0070)', () => {
  it('lose their query string and their fragment, such as the code and state of the OIDC callback', () => {
    expect(scrubUrl(`${ORIGIN}/auth/callback?code=abc&state=def`)).toBe(`${ORIGIN}/auth/callback`)
    expect(scrubUrl(`${ORIGIN}/subjects/java#chapitre-2`)).toBe(`${ORIGIN}/subjects/java`)
  })

  it('lose the token of an invitation link, in the page and in the API', () => {
    expect(scrubUrl(`${ORIGIN}/join/Zx9-kq_3TfA`)).toBe(`${ORIGIN}/join/:token`)
    expect(scrubUrl('/api/v1/invitations/Zx9-kq_3TfA/accept')).toBe(
      '/api/v1/invitations/:token/accept',
    )
  })

  it('lose the handle of a public profile, in the page and in the API', () => {
    expect(scrubUrl(`${ORIGIN}/u/alice-dupont?tab=skills`)).toBe(`${ORIGIN}/u/:handle`)
    expect(scrubUrl('/api/v1/public-profiles/alice-dupont')).toBe('/api/v1/public-profiles/:handle')
  })

  it('keep the identifiers of resources, which say nothing about anyone', () => {
    const url = `${ORIGIN}/play/live/0192f5e4-7b1a-7c3e-9d2f-1a2b3c4d5e6f`

    expect(scrubUrl(url)).toBe(url)
  })
})

describe('the messages sent to Sentry (ADR 0070)', () => {
  it('clean every address they contain', () => {
    expect(
      scrubText(
        'No route matches URL "/join/Zx9-kq_3TfA?ref=mail" nor /api/v1/public-profiles/alice',
      ),
    ).toBe('No route matches URL "/join/:token" nor /api/v1/public-profiles/:handle')
  })

  it('keep the question mark of a sentence', () => {
    expect(scrubText('Pourquoi ? Parce que')).toBe('Pourquoi ? Parce que')
  })
})

describe('the breadcrumbs sent to Sentry (ADR 0070)', () => {
  it('never come from the console', () => {
    expect(scrubBreadcrumb({ category: 'console', message: 'alice@recallio.test' })).toBeNull()
  })

  it('clean the addresses of a navigation', () => {
    expect(
      scrubBreadcrumb({
        category: 'navigation',
        data: { from: '/auth/callback?code=abc&state=def', to: '/u/alice' },
      }),
    ).toEqual({ category: 'navigation', data: { from: '/auth/callback', to: '/u/:handle' } })
  })

  it('clean the address of a call to the API, and keep the rest', () => {
    expect(
      scrubBreadcrumb({
        category: 'fetch',
        data: { method: 'POST', url: '/api/v1/invitations/Zx9-kq_3TfA/accept', status_code: 404 },
      }),
    ).toEqual({
      category: 'fetch',
      data: { method: 'POST', url: '/api/v1/invitations/:token/accept', status_code: 404 },
    })
  })
})

describe('the errors sent to Sentry (ADR 0070)', () => {
  const event: SentryEvent = {
    type: undefined,
    message: 'Échec sur /u/alice?x=1',
    request: {
      url: `${ORIGIN}/join/Zx9-kq_3TfA?utm_source=mail#top`,
      query_string: 'utm_source=mail',
      cookies: { session: 'secret' },
      headers: { Referer: `${ORIGIN}/u/alice?tab=1`, 'User-Agent': 'Firefox' },
    },
    exception: {
      values: [{ type: 'Error', value: 'Fetch /api/v1/invitations/Zx9-kq_3TfA failed' }],
    },
    breadcrumbs: [
      { category: 'console', message: 'secret' },
      { category: 'navigation', data: { from: '/', to: '/join/Zx9-kq_3TfA' } },
    ],
    user: { id: '42', ip_address: '{{auto}}' },
  }

  it('lose every sensitive part of their addresses and messages', () => {
    const scrubbed = scrubEvent(event)

    expect(scrubbed.message).toBe('Échec sur /u/:handle')
    expect(scrubbed.request).toEqual({
      url: `${ORIGIN}/join/:token`,
      headers: { Referer: `${ORIGIN}/u/:handle`, 'User-Agent': 'Firefox' },
    })
    expect(scrubbed.exception?.values?.[0].value).toBe('Fetch /api/v1/invitations/:token failed')
    expect(scrubbed.breadcrumbs).toEqual([
      { category: 'navigation', data: { from: '/', to: '/join/:token' } },
    ])
  })

  it('carry no user', () => {
    expect(scrubEvent(event)).not.toHaveProperty('user')
  })
})
