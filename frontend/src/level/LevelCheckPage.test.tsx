import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { LevelCheck, LevelCheckDomain, LevelCheckDomainResult } from '../api/levelChecks'
import { fakeSession, renderAt, sentRequests, SIGNED_IN_API, stubApi } from '../test/render'
import { TEST_TOUR } from '../tour/tourFixtures'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const LEVEL_CHECK = '/api/v1/me/level-checks/go'
const checked = (domain: string) => `POST ${LEVEL_CHECK}/domains/${domain}`

/** Un domaine d'une question à choix, dont la réponse juste est « Oui ». */
function domain(id: string, title: string, overrides: Partial<LevelCheckDomain> = {}) {
  return {
    id,
    title,
    beginnerNotions: 3,
    known: false,
    questions: [
      {
        cardId: `${id}-q`,
        type: 'MULTIPLE_CHOICE',
        prompt: `Une question sur ${title} ?`,
        choices: ['Oui', 'Non'],
        multipleAnswers: false,
        blankCount: 0,
        notionSlug: `${id}-notion`,
        notionTitle: title,
      },
    ],
    ...overrides,
  } satisfies LevelCheckDomain
}

/** Les bases de Go : un vrai ou faux (la réponse juste est « Faux ») et un code à compléter (« main »). */
const BASES = domain('bases', 'Bases du langage', {
  beginnerNotions: 9,
  questions: [
    {
      cardId: 'q1',
      type: 'TRUE_FALSE',
      prompt: '`main` prend des paramètres.',
      choices: ['Vrai', 'Faux'],
      multipleAnswers: false,
      blankCount: 0,
      notionSlug: 'hello-world',
      notionTitle: 'Le premier programme',
    },
    {
      cardId: 'q2',
      type: 'FILL_IN',
      prompt: '`package ___`',
      choices: [],
      multipleAnswers: false,
      blankCount: 1,
      notionSlug: 'packages-imports',
      notionTitle: 'Packages et imports',
    },
  ],
})

function levelCheck(overrides: Partial<LevelCheck> = {}): LevelCheck {
  return {
    subjectSlug: 'go',
    subjectTitle: 'Go',
    knownNotions: 0,
    domains: [
      BASES,
      domain('collections', 'Les collections'),
      domain('fonctions', 'Les fonctions'),
      // Réussi lors d'un test précédent.
      domain('erreurs', 'Les erreurs', { known: true }),
    ],
    ...overrides,
  }
}

function result(id: string, passed: boolean, knownNotions: number): LevelCheckDomainResult {
  return {
    domain: id,
    passed,
    corrections: [
      {
        cardId: `${id}-q`,
        correct: passed,
        correctChoices: [0],
        blanks: [],
        explanation: 'Oui, bien sûr.',
      },
    ],
    knownNotions,
  }
}

const BASES_PASSED: LevelCheckDomainResult = {
  domain: 'bases',
  passed: true,
  corrections: [
    { cardId: 'q1', correct: true, correctChoices: [1], blanks: [], explanation: 'Non.' },
    {
      cardId: 'q2',
      correct: true,
      correctChoices: [],
      blanks: [{ expected: 'main', correct: true }],
      explanation: 'Le package main.',
    },
  ],
  knownNotions: 12,
}

/** Le Tour après le test : les deux premières notions sont connues, la valeur zéro est la suivante à découvrir. */
const TOUR_API = {
  '/api/v1/subjects/go/tour': { body: TEST_TOUR },
  '/api/v1/me/tour/go': {
    body: {
      validatedNotions: [],
      knownNotions: ['hello-world', 'packages-imports'],
      lastPage: null,
    },
  },
}

describe('LevelCheckPage (ADR 0068)', () => {
  it('says what the test asks and what it changes, before the first question', async () => {
    stubApi({ ...SIGNED_IN_API, [LEVEL_CHECK]: { body: levelCheck({ knownNotions: 3 }) } })
    renderAt('/subjects/go/level-check', signedIn())

    expect(await screen.findByRole('heading', { name: 'Test de niveau : Go' })).toBeInTheDocument()
    expect(
      screen.getByText(
        'Tu connais déjà un peu Go ? Deux questions par domaine des bases ; deux domaines ratés de suite arrêtent le test. Chaque domaine réussi est sauté dans le Tour et passe après le reste en révision.',
      ),
    ).toBeVisible()
    expect(
      screen.getByText('Tu as déjà passé ce test : 3 notions sautées. Le refaire n’enlève rien.'),
    ).toBeVisible()
    expect(screen.queryByRole('button', { name: 'Valider' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Commencer le test' })).toBeEnabled()
  })

  it('asks the domains one by one, and stops after two failed in a row', async () => {
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      ...TOUR_API,
      [LEVEL_CHECK]: { body: levelCheck() },
      [checked('bases')]: { body: BASES_PASSED },
      [checked('collections')]: { body: result('collections', false, 12) },
      [checked('fonctions')]: { body: result('fonctions', false, 12) },
    })
    renderAt('/subjects/go/level-check', signedIn())

    await userEvent.click(await screen.findByRole('button', { name: 'Commencer le test' }))
    expect(
      screen.getByRole('heading', { name: 'Domaine 1 sur 4 : Bases du langage' }),
    ).toBeInTheDocument()
    const validate = screen.getByRole('button', { name: 'Valider' })
    expect(validate).toBeDisabled()
    await userEvent.click(screen.getByRole('radio', { name: 'Faux' }))
    expect(validate).toBeDisabled()
    await userEvent.type(screen.getByRole('textbox', { name: 'Trou 1' }), 'main')
    await userEvent.click(validate)

    expect(await screen.findByText('Domaine réussi : tu sautes 9 notions')).toBeInTheDocument()
    expect(screen.getByText('Le package main.')).toBeVisible()
    // Au clavier : le focus passe sur « Domaine suivant », Entrée y mène.
    expect(screen.getByRole('button', { name: 'Domaine suivant' })).toHaveFocus()
    await userEvent.keyboard('{Enter}')

    expect(
      await screen.findByRole('heading', { name: 'Domaine 2 sur 4 : Les collections' }),
    ).toBeInTheDocument()
    await userEvent.click(screen.getByRole('radio', { name: 'Non' }))
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))
    expect(await screen.findByText('À voir dans le Tour')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Domaine suivant' }))

    await screen.findByRole('heading', { name: 'Domaine 3 sur 4 : Les fonctions' })
    await userEvent.click(screen.getByRole('radio', { name: 'Non' }))
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))
    // Deux domaines ratés de suite : le test s'arrête, le quatrième n'est pas posé.
    await userEvent.click(await screen.findByRole('button', { name: 'Voir le résultat' }))

    const end = await screen.findByRole('region', { name: 'Test terminé' })
    expect(end).toHaveTextContent('12 notions sautées')
    expect(end).toHaveTextContent('Deux domaines ratés de suite : le test s’arrête là.')
    const domains = within(end).getByRole('list', { name: 'Les domaines des bases' })
    expect(
      within(domains)
        .getAllByRole('listitem')
        .map((item) => item.textContent),
    ).toEqual([
      'Bases du langageRéussi : 9 notions sautées',
      'Les collectionsÀ voir dans le Tour',
      'Les fonctionsÀ voir dans le Tour',
      'Les erreursDéjà connu, d’un test précédent',
    ])
    // Vers la première notion ni validée, ni connue.
    await waitFor(() =>
      expect(within(end).getByRole('link', { name: 'Continuer le Tour' })).toHaveAttribute(
        'href',
        '/subjects/go/tour/valeur-zero',
      ),
    )

    const [first] = sentRequests(fetchMock).filter((request) => request.method === 'POST')
    expect(await first?.json()).toEqual({
      answers: [
        { cardId: 'q1', answer: { selectedChoices: [1] } },
        { cardId: 'q2', answer: { filledBlanks: ['main'] } },
      ],
    })
  })

  it('cancels the test after a confirmation, and starts again from the introduction', async () => {
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      ...TOUR_API,
      [LEVEL_CHECK]: { body: levelCheck({ domains: [domain('collections', 'Les collections')] }) },
      [checked('collections')]: { body: result('collections', true, 3) },
      [`DELETE ${LEVEL_CHECK}`]: { status: 204, body: null },
    })
    renderAt('/subjects/go/level-check', signedIn())

    await userEvent.click(await screen.findByRole('button', { name: 'Commencer le test' }))
    await userEvent.click(screen.getByRole('radio', { name: 'Oui' }))
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Voir le résultat' }))
    const end = await screen.findByRole('region', { name: 'Test terminé' })
    expect(end).toHaveTextContent('3 notions sautées')

    await userEvent.click(within(end).getByRole('button', { name: 'Annuler le test' }))
    const confirmation = screen.getByRole('group', { name: /Annuler le test \?/ })
    // Le focus va au choix qui ne détruit rien.
    expect(within(confirmation).getByRole('button', { name: 'Garder mes résultats' })).toHaveFocus()
    expect(sentRequests(fetchMock).some((request) => request.method === 'DELETE')).toBe(false)
    await userEvent.click(
      within(confirmation).getByRole('button', { name: 'Oui, annuler le test' }),
    )

    expect(
      await screen.findByText(
        'Test annulé : le Tour et tes révisions reprennent toutes les bases.',
      ),
    ).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Commencer le test' })).toBeInTheDocument()
    await waitFor(() =>
      expect(sentRequests(fetchMock).filter((request) => request.method === 'DELETE')).toHaveLength(
        1,
      ),
    )
  })

  it('sends a report on a question without validating its domain', async () => {
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      [LEVEL_CHECK]: { body: levelCheck() },
      'POST /api/v1/content-reports': {
        status: 201,
        body: { id: 'r1', reason: 'WRONG_ANSWER', createdAt: '2026-11-20T08:00:00Z' },
      },
    })
    renderAt('/subjects/go/level-check', signedIn())

    await userEvent.click(await screen.findByRole('button', { name: 'Commencer le test' }))
    const [firstReport] = screen.getAllByRole('button', { name: 'Signaler une erreur' })
    await userEvent.click(firstReport!)
    const form = screen.getByRole('form', { name: 'Signaler une erreur' })
    await userEvent.click(within(form).getByRole('button', { name: 'Envoyer' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Merci : le signalement est envoyé')
    // Le signalement n'est pas imbriqué dans le domaine : l'envoyer ne le valide pas.
    expect(sentRequests(fetchMock).some((request) => request.url.includes('/domains/'))).toBe(false)
    expect(screen.getByRole('button', { name: 'Valider' })).toBeDisabled()
  })

  it('keeps the result list when a domain known from before is failed again', async () => {
    stubApi({
      ...SIGNED_IN_API,
      ...TOUR_API,
      [LEVEL_CHECK]: {
        body: levelCheck({
          knownNotions: 3,
          domains: [domain('collections', 'Les collections', { known: true })],
        }),
      },
      [checked('collections')]: { body: result('collections', false, 3) },
    })
    renderAt('/subjects/go/level-check', signedIn())

    await userEvent.click(await screen.findByRole('button', { name: 'Commencer le test' }))
    await userEvent.click(screen.getByRole('radio', { name: 'Non' }))
    await userEvent.click(screen.getByRole('button', { name: 'Valider' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Voir le résultat' }))

    const end = await screen.findByRole('region', { name: 'Test terminé' })
    expect(end).toHaveTextContent('3 notions sautées')
    expect(within(end).getByRole('listitem')).toHaveTextContent('Déjà connu, d’un test précédent')
  })

  it('says when the subject has no level check', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/me/level-checks/kafka': {
        status: 422,
        body: {
          status: 422,
          title: 'Requête refusée',
          code: 'level-check.unavailable',
          detail: 'Le sujet « kafka » n’a pas encore de test de niveau.',
        },
      },
    })
    renderAt('/subjects/kafka/level-check', signedIn())

    expect(await screen.findByText('Pas de test de niveau pour ce sujet')).toBeInTheDocument()
    expect(screen.getByText('Le sujet « kafka » n’a pas encore de test de niveau.')).toBeVisible()
  })
})
