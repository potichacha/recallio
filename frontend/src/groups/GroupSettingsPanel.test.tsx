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
  TEST_SUBJECTS,
} from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const JAVA_SETTINGS = {
  ...TEST_GROUP_SETTINGS,
  subjects: ['java'],
  cardTypes: ['FLASHCARD', 'CODE_OUTPUT'],
  dailyGoal: 15,
}

function group(myRole: 'OWNER' | 'MEMBER', settings: object = JAVA_SETTINGS) {
  return {
    id: 'g1',
    name: 'Équipe backend',
    createdAt: '2026-11-01T10:00:00Z',
    myRole,
    settings,
    members: [
      {
        userId: 'u1',
        displayName: 'Alice',
        role: 'OWNER',
        joinedAt: '2026-11-01T10:00:00Z',
        me: myRole === 'OWNER',
      },
    ],
  }
}

const theme = (subjectSlug: string, slug: string, title: string) => ({
  id: slug,
  subjectSlug,
  subjectTitle: subjectSlug,
  slug,
  title,
  position: 1,
  cardCount: 3,
})

const NO_INVITATION = { status: 404, body: { code: 'group.no-active-invitation' } }

describe('GroupSettingsPanel', () => {
  it('shows members what the group reviews, and lets them review with it', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/groups/g1/leaderboard': { body: TEST_LEADERBOARD },
      '/api/v1/groups/g1/challenges': { body: [] },
      '/api/v1/groups/g1': { body: group('MEMBER') },
      '/api/v1/subjects': { body: TEST_SUBJECTS },
    })
    renderAt('/groups/g1', signedIn())

    const panel = await screen.findByRole('region', { name: 'Réglages' })
    expect(await within(panel).findByText('Java')).toBeInTheDocument()
    expect(panel).toHaveTextContent("Questions techniques, Qu'affiche ce code ?")
    expect(panel).toHaveTextContent('15 cartes')
    expect(panel).toHaveTextContent('Nominatif')
    expect(within(panel).getByRole('link', { name: 'Réviser avec le groupe' })).toHaveAttribute(
      'href',
      '/review?group=g1',
    )
    expect(
      within(panel).queryByRole('button', { name: 'Modifier les réglages' }),
    ).not.toBeInTheDocument()
  })

  it('waits for the owner to choose subjects before offering a review', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/groups/g1/leaderboard': { body: TEST_LEADERBOARD },
      '/api/v1/groups/g1/challenges': { body: [] },
      '/api/v1/groups/g1': { body: group('OWNER', TEST_GROUP_SETTINGS) },
      '/api/v1/groups/g1/invitation': NO_INVITATION,
    })
    renderAt('/groups/g1', signedIn())

    const panel = await screen.findByRole('region', { name: 'Réglages' })
    expect(panel).toHaveTextContent('Aucun sujet choisi pour le moment')
    expect(panel).toHaveTextContent('Tous les formats')
    expect(
      within(panel).queryByRole('link', { name: 'Réviser avec le groupe' }),
    ).not.toBeInTheDocument()
  })

  it('lets the owner choose subjects, themes, formats, goal and leaderboard', async () => {
    const saved = {
      subjects: ['java', 'go'],
      themes: ['go-concurrence'],
      cardTypes: ['FLASHCARD'],
      dailyGoal: 20,
      leaderboardVisibility: 'ANONYMOUS',
    }
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      '/api/v1/groups/g1/leaderboard': { body: TEST_LEADERBOARD },
      '/api/v1/groups/g1/challenges': { body: [] },
      '/api/v1/groups/g1': { body: group('OWNER') },
      '/api/v1/groups/g1/invitation': NO_INVITATION,
      '/api/v1/subjects': { body: TEST_SUBJECTS },
      '/api/v1/subjects/java/themes': { body: [theme('java', 'java-bases', 'Bases')] },
      '/api/v1/subjects/go/themes': {
        body: [theme('go', 'go-bases', 'Bases'), theme('go', 'go-concurrence', 'Concurrence')],
      },
      'PUT /api/v1/groups/g1/settings': { body: group('OWNER', saved) },
    })
    renderAt('/groups/g1', signedIn())

    const panel = await screen.findByRole('region', { name: 'Réglages' })
    await userEvent.click(within(panel).getByRole('button', { name: 'Modifier les réglages' }))
    const form = within(panel).getByRole('form', { name: 'Réglages du groupe' })
    // Les brouillons ne sont pas proposés.
    expect(within(form).queryByRole('checkbox', { name: 'Rust' })).not.toBeInTheDocument()
    await userEvent.click(within(form).getByRole('checkbox', { name: 'Go' }))
    await userEvent.click(await within(form).findByRole('checkbox', { name: 'Concurrence' }))
    await userEvent.click(within(form).getByRole('checkbox', { name: "Qu'affiche ce code ?" }))
    const goal = within(form).getByLabelText('Objectif quotidien (cartes)')
    await userEvent.clear(goal)
    await userEvent.type(goal, '20')
    await userEvent.click(within(form).getByRole('radio', { name: 'Anonymisé' }))
    await userEvent.click(within(form).getByRole('button', { name: 'Enregistrer les réglages' }))

    expect(await within(panel).findByText('Anonymisé')).toBeInTheDocument()
    const put = sentRequests(fetchMock).find((request) => request.method === 'PUT')
    expect(await put?.json()).toEqual(saved)
  })
})
