import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  fakeSession,
  renderAt,
  sentRequests,
  SIGNED_IN_API,
  stubApi,
  TEST_GROUP_SETTINGS,
  TEST_LEADERBOARD,
} from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const group = (myRole: 'OWNER' | 'MEMBER') => ({
  id: 'g1',
  name: 'Équipe backend',
  createdAt: '2026-11-01T10:00:00Z',
  myRole,
  settings: TEST_GROUP_SETTINGS,
  members: [],
})

const challenge = (overrides: object) => ({
  id: 'c1',
  title: 'Défi du lundi',
  cardCount: 5,
  secondsPerCard: 30,
  endsAt: '2026-11-09T10:00:00Z',
  open: true,
  myStatus: 'NOT_STARTED',
  myScore: null,
  ...overrides,
})

function stubGroup(myRole: 'OWNER' | 'MEMBER', challenges: object[], extra: object = {}) {
  return stubApi({
    ...SIGNED_IN_API,
    '/api/v1/groups/g1': { body: group(myRole) },
    '/api/v1/groups/g1/invitation': { status: 404, body: { code: 'group.no-active-invitation' } },
    '/api/v1/groups/g1/leaderboard': { body: TEST_LEADERBOARD },
    '/api/v1/groups/g1/challenges': { body: challenges },
    ...extra,
  })
}

describe('ChallengesPanel', () => {
  it('lists the challenges with what the member can do next', async () => {
    stubGroup('MEMBER', [
      challenge({ id: 'c1', title: 'À jouer' }),
      challenge({ id: 'c2', title: 'Commencé', myStatus: 'IN_PROGRESS' }),
      challenge({ id: 'c3', title: 'Joué', myStatus: 'FINISHED', myScore: 4 }),
      challenge({ id: 'c4', title: 'Fermé', open: false }),
    ])
    renderAt('/groups/g1', signedIn())

    const panel = await screen.findByRole('region', { name: 'Défis' })
    const links = await within(panel).findAllByRole('link')
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      '/challenges/c1',
      '/challenges/c2',
      '/challenges/c3',
      '/challenges/c4',
    ])
    expect(links[0]).toHaveTextContent('À jouer5 QCM · 30 s par carte')
    expect(links[0]).toHaveTextContent('Jouer')
    expect(links[1]).toHaveTextContent('Reprendre')
    expect(links[2]).toHaveTextContent('Score : 4 / 5')
    expect(links[3]).toHaveTextContent('Terminé')
    expect(within(panel).queryByRole('form', { name: 'Lancer un défi' })).not.toBeInTheDocument()
  })

  it('lets the owner launch a challenge, and explains a refusal', async () => {
    const fetchMock = stubGroup('OWNER', [], {
      'POST /api/v1/groups/g1/challenges': {
        status: 422,
        body: {
          code: 'group.not-enough-cards',
          detail: 'Les sujets du groupe ne comptent que 3 QCM.',
        },
      },
    })
    renderAt('/groups/g1', signedIn())

    const form = await screen.findByRole('form', { name: 'Lancer un défi' })
    await userEvent.type(within(form).getByLabelText('Titre du défi'), 'Défi du lundi')
    const seconds = within(form).getByLabelText('Secondes par carte')
    await userEvent.clear(seconds)
    await userEvent.type(seconds, '20')
    await userEvent.click(within(form).getByRole('button', { name: 'Lancer le défi' }))

    expect(await within(form).findByRole('alert')).toHaveTextContent(
      'Les sujets du groupe ne comptent que 3 QCM.',
    )
    const post = sentRequests(fetchMock).find((request) => request.method === 'POST')
    expect(await post?.json()).toEqual({ title: 'Défi du lundi', cardCount: 5, secondsPerCard: 20 })
  })
})
