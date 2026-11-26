import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  fakeSession,
  renderAt,
  SIGNED_IN_API,
  stubApi,
  TEST_PROFILE,
  TEST_SUBJECTS,
} from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const CATALOGUE = { '/api/v1/subjects': { body: TEST_SUBJECTS } }

describe('the door « S’entraîner » (ADR 0077)', () => {
  it('offers the mock exam and the mistakes of each followed subject', async () => {
    stubApi({
      ...SIGNED_IN_API,
      ...CATALOGUE,
      '/api/v1/me': { body: { ...TEST_PROFILE, followedSubjects: ['java', 'go'] } },
    })
    renderAt('/practice', signedIn())

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Comme en entretien' }),
    ).toBeInTheDocument()
    const java = await screen.findByRole('region', { name: 'Java' })
    expect(within(java).getByRole('link', { name: /^Examen blanc/ })).toHaveAttribute(
      'href',
      '/subjects/java/exam',
    )
    expect(within(java).getByRole('link', { name: /^Carnet d’erreurs/ })).toHaveAttribute(
      'href',
      '/subjects/java/mistakes',
    )
    const go = screen.getByRole('region', { name: 'Go' })
    expect(within(go).getByRole('link', { name: /^Examen blanc/ })).toHaveAttribute(
      'href',
      '/subjects/go/exam',
    )
    // Un sujet du catalogue qu'on ne suit pas n'est pas proposé.
    expect(screen.queryByRole('region', { name: 'Rust' })).not.toBeInTheDocument()
  })

  it('does not announce the intensive review, which does not exist yet', async () => {
    stubApi({ ...SIGNED_IN_API, ...CATALOGUE })
    renderAt('/practice', signedIn())

    await screen.findByRole('region', { name: 'Java' })
    expect(screen.getByRole('main')).not.toHaveTextContent(/intensive|sans fin/i)
  })

  it('leads who follows no subject to choose some', async () => {
    stubApi({
      ...SIGNED_IN_API,
      ...CATALOGUE,
      '/api/v1/me': { body: { ...TEST_PROFILE, followedSubjects: [] } },
    })
    renderAt('/practice', signedIn())

    expect(await screen.findByText('Tu ne suis encore aucun sujet.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Choisir mes sujets' })).toHaveAttribute(
      'href',
      '/profile/subjects',
    )
  })

  it('offers to retry when the followed subjects do not load', async () => {
    stubApi({
      ...CATALOGUE,
      '/api/v1/me': { status: 500, body: { status: 500 } },
      '/api/v1/me/progress': SIGNED_IN_API['/api/v1/me/progress'],
    })
    renderAt('/practice', signedIn())

    expect(await screen.findByText('Impossible de charger tes sujets.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Réessayer' })).toBeInTheDocument()
  })

  it('invites a visitor to sign in, coming back to the door', async () => {
    const session = fakeSession()
    renderAt('/practice', session)

    const main = within(screen.getByRole('main'))
    expect(await main.findByRole('heading', { name: 'Connexion requise' })).toBeInTheDocument()
    await userEvent.click(main.getByRole('button', { name: 'Se connecter' }))
    expect(session.login).toHaveBeenCalledWith('/practice')
  })
})
