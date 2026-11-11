import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, stubApi, TEST_SUBJECTS } from '../test/render'

const SUBJECTS = { '/api/v1/subjects': { body: TEST_SUBJECTS } }

describe('HomePage', () => {
  it('shows nothing about the API to a visitor', async () => {
    stubApi(SUBJECTS)
    renderAt('/')

    await screen.findByRole('region', { name: 'Essaie sans compte' })
    expect(screen.queryByText(/API/)).not.toBeInTheDocument()
  })

  it('offers to try every published subject with a Tour, without an account', async () => {
    stubApi(SUBJECTS)
    renderAt('/')

    const trial = await screen.findByRole('region', { name: 'Essaie sans compte' })
    expect(screen.getByRole('link', { name: 'Essayer Java' })).toHaveAttribute('href', '/try/java')
    expect(screen.getByRole('link', { name: 'Essayer Go' })).toHaveAttribute('href', '/try/go')
    expect(trial).not.toHaveTextContent('Rust')
  })

  it('signs in, or shows what there is to learn', async () => {
    stubApi(SUBJECTS)
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
