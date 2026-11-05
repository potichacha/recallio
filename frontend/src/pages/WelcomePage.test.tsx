import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  fakeSession,
  renderAt,
  sentRequests,
  SIGNED_IN_API,
  stubApi,
  TEST_PROFILE,
  TEST_SUBJECTS,
} from '../test/render'

const NEW_USER = { ...TEST_PROFILE, followedSubjects: [] }
const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

describe('WelcomePage', () => {
  it('leads a user who follows no subject to the choice of subjects', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/me': { body: NEW_USER },
      '/api/v1/subjects': { body: TEST_SUBJECTS },
    })

    const router = renderAt('/', signedIn())

    expect(
      await screen.findByRole('heading', { name: 'Quels sujets veux-tu réviser ?' }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/welcome')
  })

  it('offers every published subject and starts reviewing the chosen ones', async () => {
    const following = { ...NEW_USER, followedSubjects: ['go'] }
    const api = {
      ...SIGNED_IN_API,
      '/api/v1/me': { body: NEW_USER },
      '/api/v1/subjects': { body: TEST_SUBJECTS },
      'PUT /api/v1/me/followed-subjects': { body: following },
      '/api/v1/review-queue': {
        body: { cards: [], dueCount: 0, newCount: 0, nextDueAt: null },
      },
    }
    const fetchMock = stubApi(api)
    const router = renderAt('/welcome', signedIn())

    const start = await screen.findByRole('button', { name: 'Commencer à réviser' })
    expect(start).toBeDisabled()
    expect(await screen.findByRole('checkbox', { name: /Java/ })).not.toBeChecked()
    expect(screen.queryByRole('checkbox', { name: /Rust/ })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('checkbox', { name: /Go/ }))
    // Le serveur renvoie désormais le profil avec ses sujets.
    api['/api/v1/me'] = { body: following }
    await userEvent.click(start)

    // La première session part aussitôt, sur le sujet choisi, pour la durée proposée.
    expect(await screen.findByText('Tout est à jour en Go pour le moment.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/review')
    expect(router.state.location.search).toBe('?subject=go&minutes=10&mode=all')
    const put = sentRequests(fetchMock).find((request) => request.method === 'PUT')
    expect(await put?.json()).toEqual({ subjects: ['go'] })
  })

  it('remembers the daily time chosen as the length of the sessions', async () => {
    const following = { ...NEW_USER, followedSubjects: ['java'] }
    const api = {
      ...SIGNED_IN_API,
      '/api/v1/me': { body: NEW_USER },
      '/api/v1/subjects': { body: TEST_SUBJECTS },
      'PUT /api/v1/me/followed-subjects': { body: following },
      '/api/v1/review-queue': {
        body: { cards: [], dueCount: 0, newCount: 0, nextDueAt: null },
      },
    }
    stubApi(api)
    const router = renderAt('/welcome', signedIn())

    await userEvent.click(await screen.findByRole('checkbox', { name: /Java/ }))
    const time = screen.getByRole('group', { name: 'Combien de temps par jour ?' })
    expect(within(time).getByRole('radio', { name: '10 min' })).toBeChecked()
    await userEvent.click(within(time).getByRole('radio', { name: '5 min' }))
    api['/api/v1/me'] = { body: following }
    await userEvent.click(screen.getByRole('button', { name: 'Commencer à réviser' }))

    expect(await screen.findByText('Tout est à jour en Java pour le moment.')).toBeInTheDocument()
    expect(router.state.location.search).toBe('?subject=java&minutes=5&mode=all')
    expect(JSON.parse(localStorage.getItem('recallio.review-session') ?? 'null')).toEqual({
      subject: 'java',
      minutes: 5,
      mode: 'all',
    })
  })
})
