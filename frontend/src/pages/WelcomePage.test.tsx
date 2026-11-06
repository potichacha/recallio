import { screen, waitFor } from '@testing-library/react'
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

const NEW_USER = { ...TEST_PROFILE, followedSubjects: [], tracks: {} }
const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

/** « Programmer de zéro », une fois publié : il ouvre le premier but. */
const BEGINNER = {
  ...TEST_SUBJECTS[0],
  id: 'programmer-de-zero',
  slug: 'programmer-de-zero',
  title: 'Programmer de zéro',
  category: 'FUNDAMENTALS',
}

describe('WelcomePage', () => {
  it('leads a user who follows no subject to the question of their goal', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/me': { body: NEW_USER },
      '/api/v1/subjects': { body: TEST_SUBJECTS },
    })

    const router = renderAt('/', signedIn())

    expect(
      await screen.findByRole('heading', { name: 'Qu’est-ce qui t’amène ?' }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/welcome')
  })

  it('sends who prepares an interview to the length of their first session', async () => {
    const following = { ...NEW_USER, followedSubjects: ['go'] }
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      '/api/v1/me': { body: NEW_USER },
      '/api/v1/subjects': { body: TEST_SUBJECTS },
      'PUT /api/v1/me/followed-subjects': { body: following },
    })
    const router = renderAt('/welcome', signedIn())

    const start = await screen.findByRole('button', { name: 'C’est parti' })
    expect(start).toBeDisabled()
    await userEvent.click(screen.getByRole('radio', { name: /Je prépare un entretien/ }))
    // Rust est en brouillon : il n'est pas proposé.
    expect(screen.queryByRole('radio', { name: /Rust/ })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('radio', { name: /Go/ }))
    await userEvent.click(start)

    expect(await screen.findByRole('heading', { name: 'Réviser Go' })).toBeInTheDocument()
    expect(router.state.location.search).toBe('?subject=go')
    const put = sentRequests(fetchMock).find((request) => request.method === 'PUT')
    expect(await put?.json()).toEqual({ subjects: ['go'] })
  })

  it('sends who knows another language to the Tour of the new one, on the learning track', async () => {
    const following = { ...NEW_USER, followedSubjects: ['java'] }
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      '/api/v1/me': { body: NEW_USER },
      '/api/v1/subjects': { body: TEST_SUBJECTS },
      'PUT /api/v1/me/followed-subjects': { body: following },
      'PUT /api/v1/me/followed-subjects/java/track': {
        body: { ...following, tracks: { java: 'LEARN' } },
      },
      '/api/v1/subjects/java/tour': { status: 404, body: { status: 404 } },
    })
    const router = renderAt('/welcome', signedIn())

    await userEvent.click(
      await screen.findByRole('radio', { name: /Je connais déjà un autre langage/ }),
    )
    await userEvent.click(screen.getByRole('radio', { name: /Java/ }))
    await userEvent.click(screen.getByRole('button', { name: 'C’est parti' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/subjects/java/tour'))
    const track = sentRequests(fetchMock).find((request) => request.url.endsWith('/track'))
    expect(await track?.json()).toEqual({ track: 'LEARN' })
  })

  it('offers to start from scratch only once « Programmer de zéro » is open', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/me': { body: NEW_USER },
      '/api/v1/subjects': { body: TEST_SUBJECTS },
    })
    renderAt('/welcome', signedIn())

    expect(
      await screen.findByRole('radio', { name: /Je connais déjà un autre langage/ }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('radio', { name: /Je n’ai jamais programmé/ }),
    ).not.toBeInTheDocument()
  })

  it('starts from scratch in the Tour of « Programmer de zéro », without asking a subject', async () => {
    const following = { ...NEW_USER, followedSubjects: ['programmer-de-zero'] }
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      '/api/v1/me': { body: NEW_USER },
      '/api/v1/subjects': { body: [...TEST_SUBJECTS, BEGINNER] },
      'PUT /api/v1/me/followed-subjects': { body: following },
      'PUT /api/v1/me/followed-subjects/programmer-de-zero/track': { body: following },
      '/api/v1/subjects/programmer-de-zero/tour': { status: 404, body: { status: 404 } },
    })
    const router = renderAt('/welcome', signedIn())

    await userEvent.click(await screen.findByRole('radio', { name: /Je n’ai jamais programmé/ }))
    expect(screen.queryByRole('group', { name: /Quel langage/ })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'C’est parti' }))

    await waitFor(() =>
      expect(router.state.location.pathname).toBe('/subjects/programmer-de-zero/tour'),
    )
    const put = sentRequests(fetchMock).find((request) => request.method === 'PUT')
    expect(await put?.json()).toEqual({ subjects: ['programmer-de-zero'] })
  })
})
