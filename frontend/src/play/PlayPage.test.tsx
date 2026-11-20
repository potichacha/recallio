import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, SIGNED_IN_API, stubApi, TEST_SUBJECTS } from '../test/render'

const summary = (subjectSlug: string, subjectTitle: string, overrides = {}) => ({
  subjectSlug,
  subjectTitle,
  day: '2026-11-20',
  state: 'NOT_STARTED',
  questionCount: 5,
  ...overrides,
})

describe('PlayPage', () => {
  it('lists the daily challenge of each subject, with where the user stands (ADR 0067)', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/subjects': { body: TEST_SUBJECTS },
      '/api/v1/me/daily-challenges': {
        body: [
          summary('java', 'Java', { state: 'DONE', score: 4 }),
          summary('go', 'Go', { state: 'IN_PROGRESS' }),
          summary('python', 'Python'),
        ],
      },
    })
    renderAt('/play', fakeSession({ status: 'authenticated', userName: 'Alice' }))

    const section = await screen.findByRole('region', { name: 'Défi du jour' })
    expect(await within(section).findByRole('link', { name: 'Java Fait : 4/5' })).toHaveAttribute(
      'href',
      '/daily/java',
    )
    expect(within(section).getByRole('link', { name: 'Go En cours' })).toHaveAttribute(
      'href',
      '/daily/go',
    )
    expect(within(section).getByRole('link', { name: 'Python À jouer' })).toHaveAttribute(
      'href',
      '/daily/python',
    )
  })

  it('invites a visitor to sign in to play the daily challenge', async () => {
    stubApi({ '/api/v1/subjects': { body: TEST_SUBJECTS } })
    const session = fakeSession()
    renderAt('/play', session)

    const section = await screen.findByRole('region', { name: 'Défi du jour' })
    await userEvent.click(within(section).getByRole('button', { name: 'Connecte-toi pour jouer' }))
    expect(session.login).toHaveBeenCalledWith('/play')
  })
})
