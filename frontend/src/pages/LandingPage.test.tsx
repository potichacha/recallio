import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderAt, sentRequests, stubApi, TEST_SUBJECTS } from '../test/render'

const card = (overrides: Record<string, unknown>) => ({
  answer: null,
  choices: [],
  multipleAnswers: false,
  blankCount: 0,
  exercise: null,
  ...overrides,
})

/** L'essai de Java, le premier sujet publié qui a un Tour : deux questions. */
const TRIAL = {
  questions: [
    {
      notion: 'egalite',
      card: card({
        cardId: 'c1',
        type: 'TRUE_FALSE',
        prompt: '`"a" == new String("a")` vaut `true`.',
        choices: ['Vrai', 'Faux'],
      }),
    },
    {
      notion: 'types',
      card: card({
        cardId: 'c2',
        type: 'TRUE_FALSE',
        prompt: '`int` est un type primitif.',
        choices: ['Vrai', 'Faux'],
      }),
    },
  ],
}

const API = {
  '/api/v1/subjects': { body: TEST_SUBJECTS },
  '/api/v1/subjects/java/trial': { body: TRIAL },
}

describe('LandingPage (ADR 0058)', () => {
  it('asks a real question at once, corrected by the server, then goes on with the trial', async () => {
    const fetchMock = stubApi({
      ...API,
      'POST /api/v1/subjects/java/trial/egalite/checks': {
        body: { cardId: 'c1', correct: true, correctChoices: [1], blanks: [], explanation: 'Non.' },
      },
    })
    renderAt('/')

    const trial = within(await screen.findByRole('region', { name: 'Essaie sans compte' }))
    await userEvent.click(await trial.findByRole('radio', { name: /Faux/ }))
    await userEvent.click(trial.getByRole('button', { name: 'Vérifier' }))

    expect(await trial.findByText('Juste !')).toBeInTheDocument()
    const check = sentRequests(fetchMock).find((request) => request.method === 'POST')
    expect(await check?.json()).toEqual({ cardId: 'c1', selectedChoices: [1] })

    // L'essai reprend à la deuxième question, la première comptée juste.
    await userEvent.click(trial.getByRole('link', { name: 'Continuer : encore 1 question' }))
    expect(await screen.findByRole('heading', { name: 'Essai : Java' })).toBeInTheDocument()
    expect(screen.getByText('Question 2 sur 2')).toBeInTheDocument()
    expect(screen.getByText('int')).toBeInTheDocument()
  })

  it('still offers the trial of every published subject with a Tour', async () => {
    stubApi(API)
    renderAt('/')

    const trial = within(await screen.findByRole('region', { name: 'Essaie sans compte' }))
    expect(await trial.findByRole('link', { name: 'Essayer Java' })).toHaveAttribute(
      'href',
      '/try/java',
    )
    expect(trial.getByRole('link', { name: 'Essayer Go' })).toHaveAttribute('href', '/try/go')
    expect(trial.queryByRole('link', { name: 'Essayer Rust' })).not.toBeInTheDocument()
  })

  it('does without the question when the trial is not available', async () => {
    stubApi({
      '/api/v1/subjects': { body: TEST_SUBJECTS },
      '/api/v1/subjects/java/trial': {
        status: 429,
        body: { status: 429, code: 'too-many-requests' },
      },
    })
    renderAt('/')

    const trial = within(await screen.findByRole('region', { name: 'Essaie sans compte' }))
    expect(await trial.findByRole('link', { name: 'Essayer Java' })).toBeInTheDocument()
    expect(trial.queryByRole('button', { name: 'Vérifier' })).not.toBeInTheDocument()
  })

  it('gives the figures of the published catalogue, from the server', async () => {
    stubApi(API)
    renderAt('/')

    // Java et Go, publiés : 20 cartes, 5 notions et 1 exercice chacun ; Rust, en brouillon, ne compte pas.
    const figures = within(await screen.findByRole('list', { name: 'Le catalogue en chiffres' }))
    expect(figures.getByText('40')).toBeInTheDocument()
    expect(figures.getByText('10')).toBeInTheDocument()
    expect(figures.getByText('2')).toBeInTheDocument()
    expect(figures.getByText('exercices de code')).toBeInTheDocument()
  })

  it('tells who it is for, how it works and what there is to learn, under a single main title', async () => {
    stubApi(API)
    renderAt('/')

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: 'Prêt pour ton entretien technique, dix minutes par jour.',
      }),
    ).toBeInTheDocument()
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1)
    expect(
      within(screen.getByRole('region', { name: 'Pour qui' })).getAllByRole('listitem'),
    ).toHaveLength(3)
    const steps = within(screen.getByRole('region', { name: 'Comment ça marche' }))
    expect(
      steps.getByRole('heading', { name: '3. La carte revient au bon moment' }),
    ).toBeInTheDocument()
    const subjects = within(await screen.findByRole('region', { name: 'Ce qu’on y apprend' }))
    expect(subjects.getByRole('link', { name: /^Java/ })).toHaveAttribute('href', '/subjects/java')
    // Un sujet en préparation est annoncé, sans lien.
    expect(subjects.getByRole('button', { name: /Rust/ })).toBeInTheDocument()
  })
})
