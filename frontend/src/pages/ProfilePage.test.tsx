import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  fakeSession,
  renderAt,
  sentRequests,
  SIGNED_IN_API,
  stubApi,
  TEST_SUBJECTS,
} from '../test/render'

const PROFILE = {
  id: 'u1',
  displayName: 'Alice Martin',
  timezone: 'Europe/Paris',
  roles: ['admin'],
  followedSubjects: ['java'],
  tracks: { java: 'PREPARE' },
}
const SUBJECTS = { '/api/v1/subjects': { body: TEST_SUBJECTS } }
const SIGNED_IN = fakeSession({ status: 'authenticated', userName: 'Alice Martin' })

describe('ProfilePage', () => {
  it('asks anonymous visitors to sign in and comes back to the profile afterwards', async () => {
    const session = fakeSession()
    renderAt('/profile', session)

    await userEvent.click(screen.getAllByRole('button', { name: 'Se connecter' })[1])

    expect(session.login).toHaveBeenCalledWith('/profile')
  })

  it('loads the profile and sends the browser timezone for the first login', async () => {
    const fetchMock = stubApi({ ...SUBJECTS, '/api/v1/me': { body: PROFILE } })

    renderAt('/profile', SIGNED_IN)

    expect(await screen.findByLabelText('Nom affiché')).toHaveValue('Alice Martin')
    expect(screen.getByLabelText('Fuseau horaire')).toHaveValue('Europe/Paris')
    expect(screen.getByText('Vous êtes administrateur.')).toBeInTheDocument()
    expect(sentRequests(fetchMock)[0].headers.get('X-Timezone')).toBeTruthy()
  })

  it('saves the new preferences', async () => {
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      ...SUBJECTS,
      '/api/v1/me': { body: PROFILE },
      'PUT /api/v1/me': { body: { ...PROFILE, displayName: 'Alice M.' } },
    })
    renderAt('/profile', SIGNED_IN)

    const name = await screen.findByLabelText('Nom affiché')
    await userEvent.clear(name)
    await userEvent.type(name, 'Alice M.')
    await userEvent.click(screen.getByRole('button', { name: 'Enregistrer' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Profil enregistré.')
    const put = sentRequests(fetchMock).find((request) => request.method === 'PUT')
    expect(await put?.json()).toEqual({ displayName: 'Alice M.', timezone: 'Europe/Paris' })
  })

  it('shows the error returned by the API', async () => {
    stubApi({
      ...SIGNED_IN_API,
      ...SUBJECTS,
      '/api/v1/me': { body: PROFILE },
      'PUT /api/v1/me': {
        status: 422,
        body: {
          type: 'about:blank',
          title: 'Requête refusée',
          status: 422,
          code: 'user.invalid-timezone',
          detail: 'Fuseau horaire inconnu.',
        },
      },
    })
    renderAt('/profile', SIGNED_IN)

    await userEvent.click(await screen.findByRole('button', { name: 'Enregistrer' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Fuseau horaire inconnu.')
  })

  it('changes the followed subjects, published ones only', async () => {
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      ...SUBJECTS,
      '/api/v1/me': { body: PROFILE },
      'PUT /api/v1/me/followed-subjects': {
        body: { ...PROFILE, followedSubjects: ['java', 'go'] },
      },
    })
    renderAt('/profile', SIGNED_IN)

    const subjects = await screen.findByRole('form', { name: 'Mes sujets' })
    expect(await within(subjects).findByRole('checkbox', { name: /Java/ })).toBeChecked()
    expect(within(subjects).queryByRole('checkbox', { name: /Rust/ })).not.toBeInTheDocument()
    await userEvent.click(within(subjects).getByRole('checkbox', { name: /Go/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Enregistrer mes sujets' }))

    expect(await screen.findByText('Sujets enregistrés.')).toBeInTheDocument()
    const put = sentRequests(fetchMock).find((request) =>
      request.url.endsWith('/followed-subjects'),
    )
    expect(await put?.json()).toEqual({ subjects: ['java', 'go'] })
  })

  it('chooses the track of a followed subject', async () => {
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      ...SUBJECTS,
      '/api/v1/me': { body: PROFILE },
      'PUT /api/v1/me/followed-subjects/java/track': {
        body: { ...PROFILE, tracks: { java: 'LEARN' } },
      },
    })
    renderAt('/profile', SIGNED_IN)

    const tracks = await screen.findByRole('region', { name: 'Mes parcours' })
    const java = await within(tracks).findByRole('combobox', { name: 'Java' })
    expect(java).toHaveValue('PREPARE')
    await userEvent.selectOptions(java, 'Apprendre')

    expect(await screen.findByText('Parcours enregistré.')).toBeInTheDocument()
    expect(java).toHaveValue('LEARN')
    const put = sentRequests(fetchMock).find((request) => request.url.endsWith('/java/track'))
    expect(await put?.json()).toEqual({ track: 'LEARN' })
  })

  it('lists the achievements, unlocked or not, and what unlocks them', async () => {
    stubApi({
      ...SIGNED_IN_API,
      ...SUBJECTS,
      '/api/v1/me': { body: PROFILE },
      '/api/v1/me/achievements': {
        body: [
          { achievement: 'FIRST_REVIEW', unlockedAt: '2026-11-02T08:00:00Z' },
          { achievement: 'STREAK_7', unlockedAt: null },
        ],
      },
    })
    renderAt('/profile', SIGNED_IN)

    const section = await screen.findByRole('region', { name: 'Mes succès' })
    expect(section).toHaveTextContent('1 / 2 débloqués')
    const [first, streak] = within(section).getAllByRole('listitem')
    expect(first).toHaveTextContent('Premier pas')
    expect(first).toHaveTextContent('Débloqué le 2 novembre 2026')
    expect(streak).toHaveTextContent('Tenir une série de 7 jours.')
    expect(streak).toHaveTextContent('À débloquer')
  })

  it('keeps the public profile private until the user makes it public', async () => {
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      ...SUBJECTS,
      '/api/v1/me': { body: PROFILE },
      '/api/v1/me/public-profile': { body: { handle: null, visible: false } },
      'PUT /api/v1/me/public-profile': { body: { handle: 'alice-dev', visible: true } },
    })
    renderAt('/profile', SIGNED_IN)

    const form = await screen.findByRole('form', { name: 'Profil public' })
    const visible = within(form).getByRole('checkbox', { name: 'Rendre mon profil public' })
    expect(visible).not.toBeChecked()
    await userEvent.type(within(form).getByLabelText('Adresse du profil'), 'Alice-Dev')
    await userEvent.click(visible)
    await userEvent.click(
      within(form).getByRole('button', { name: 'Enregistrer mon profil public' }),
    )

    expect(await within(form).findByRole('status')).toHaveTextContent('Votre profil est public.')
    const put = sentRequests(fetchMock).find(
      (request) => request.method === 'PUT' && request.url.endsWith('/public-profile'),
    )
    expect(await put?.json()).toEqual({ handle: 'alice-dev', visible: true })
  })
})
