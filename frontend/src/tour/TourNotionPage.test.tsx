import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fakeSession, renderAt, sentRequests, SIGNED_IN_API, stubApi } from '../test/render'
import { checkpoint, checkResult, HELLO_CODE, TEST_TOUR, tourNotion } from './tourFixtures'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

const TOUR_API = {
  '/api/v1/subjects/go/tour': { body: TEST_TOUR },
  '/api/v1/subjects/go/tour/hello-world': {
    body: tourNotion('hello-world', 'Le premier programme', 'Le premier programme'),
  },
  '/api/v1/subjects/go/tour/packages-imports': {
    body: tourNotion('packages-imports', 'Packages et imports', 'Packages et imports', 'Les alias'),
  },
  '/api/v1/subjects/go/tour/valeur-zero': {
    body: tourNotion('valeur-zero', 'La valeur zéro', 'La valeur zéro'),
  },
  '/api/v1/me/tour/go/hello-world': { body: checkpoint() },
  '/api/v1/me/tour/go/packages-imports': { body: checkpoint() },
  '/api/v1/me/tour/go/valeur-zero': { body: checkpoint() },
}

const COMPLETED = {
  status: 'COMPLETED',
  exitCode: 0,
  stdout: 'Bonjour, Go !\n',
  stderr: '',
  truncated: false,
  message: '',
}

describe('TourNotionPage', () => {
  it('shows the text and the program of the page, then runs it in the sandbox', async () => {
    const fetchMock = stubApi({
      ...SIGNED_IN_API,
      ...TOUR_API,
      'POST /api/v1/playground-runs': { body: COMPLETED },
    })
    renderAt('/subjects/go/tour/hello-world', signedIn())

    expect(await screen.findByRole('heading', { name: 'Le premier programme' })).toBeInTheDocument()
    expect(screen.getByText('Le premier programme', { selector: 'strong' })).toBeInTheDocument()
    expect(await screen.findByText('Page 1 sur 4')).toBeInTheDocument()
    expect(await screen.findByRole('textbox', { name: 'main.go' })).toHaveTextContent(
      'fmt.Println("Bonjour, Go !")',
    )

    await userEvent.click(screen.getByRole('button', { name: 'Exécuter' }))

    const output = await screen.findByRole('region', { name: 'Sortie du programme' })
    expect(within(output).getByLabelText('Sortie standard')).toHaveTextContent('Bonjour, Go !')
    const run = sentRequests(fetchMock).find((request) =>
      request.url.endsWith('/api/v1/playground-runs'),
    )
    expect(await run?.json()).toEqual({ files: [{ path: 'main.go', content: HELLO_CODE }] })
  })

  it('links to the previous and the next page, across the notions of the Tour', async () => {
    stubApi({ ...SIGNED_IN_API, ...TOUR_API })
    renderAt('/subjects/go/tour/packages-imports', signedIn())

    expect(await screen.findByText('Page 2 sur 4')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Page précédente' })).toHaveAttribute(
      'href',
      '/subjects/go/tour/hello-world',
    )
    await userEvent.click(screen.getByRole('link', { name: 'Page suivante' }))

    expect(await screen.findByRole('heading', { name: 'Les alias' })).toBeInTheDocument()
    expect(screen.getByText('Page 3 sur 4')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Page suivante' })).toHaveAttribute(
      'href',
      '/subjects/go/tour/valeur-zero',
    )
  })

  it('leads back to the outline from the last page', async () => {
    stubApi({ ...SIGNED_IN_API, ...TOUR_API })
    renderAt('/subjects/go/tour/valeur-zero', signedIn())

    expect(await screen.findByText('Page 4 sur 4')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Page suivante' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Retour au sommaire' })).toHaveAttribute(
      'href',
      '/subjects/go/tour',
    )
  })

  it('asks a visitor to sign in before running the program', async () => {
    stubApi(TOUR_API)
    const session = fakeSession()
    renderAt('/subjects/go/tour/hello-world', session)

    await userEvent.click(await screen.findByRole('button', { name: 'Se connecter pour exécuter' }))

    expect(session.login).toHaveBeenCalledWith('/subjects/go/tour/hello-world')
    expect(screen.queryByRole('button', { name: 'Exécuter' })).not.toBeInTheDocument()
  })

  it('shows the compilation errors, and why a run could not happen', async () => {
    const responses: Parameters<typeof stubApi>[0] = {
      ...SIGNED_IN_API,
      ...TOUR_API,
      'POST /api/v1/playground-runs': {
        body: {
          ...COMPLETED,
          status: 'COMPILATION_ERROR',
          stdout: '',
          message: 'main.go:4:2: declared and not used: x',
        },
      },
    }
    stubApi(responses)
    renderAt('/subjects/go/tour/hello-world', signedIn())

    await userEvent.click(await screen.findByRole('button', { name: 'Exécuter' }))
    expect(await screen.findByText('Le code ne compile pas.')).toBeInTheDocument()
    expect(screen.getByText('main.go:4:2: declared and not used: x')).toBeInTheDocument()

    responses['POST /api/v1/playground-runs'] = {
      status: 503,
      body: {
        status: 503,
        code: 'review.runner-unavailable',
        detail: 'L’exécution du code est indisponible pour l’instant.',
      },
    }
    await userEvent.click(screen.getByRole('button', { name: 'Exécuter' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'L’exécution du code est indisponible pour l’instant.',
    )
  })

  it('validates the notion once every answer is right, after a retry', async () => {
    const responses: Parameters<typeof stubApi>[0] = {
      ...SIGNED_IN_API,
      ...TOUR_API,
      'POST /api/v1/me/tour/go/hello-world/checks': { body: checkResult(false) },
    }
    const fetchMock = stubApi(responses)
    renderAt('/subjects/go/tour/hello-world', signedIn())

    const check = await screen.findByRole('region', { name: 'Valider la notion' })
    await userEvent.click(within(check).getByRole('radio', { name: 'Vrai' }))
    await userEvent.type(within(check).getByRole('textbox', { name: 'Trou 1' }), 'fmt')
    await userEvent.click(within(check).getByRole('button', { name: 'Vérifier' }))

    expect(await within(check).findByText('Attendu : main')).toBeInTheDocument()
    expect(within(check).getAllByText('Pas tout à fait.')).toHaveLength(2)
    expect(within(check).getByText('Bonne réponse, non cochée')).toBeInTheDocument()
    const sent = sentRequests(fetchMock).find((request) => request.url.endsWith('/checks'))
    expect(await sent?.json()).toEqual({
      answers: [
        { cardId: 'q1', selectedChoices: [0] },
        { cardId: 'q2', filledBlanks: ['fmt'] },
      ],
    })

    responses['POST /api/v1/me/tour/go/hello-world/checks'] = { body: checkResult(true) }
    await userEvent.click(within(check).getByRole('button', { name: 'Réessayer' }))
    await userEvent.click(within(check).getByRole('radio', { name: 'Faux' }))
    const blank = within(check).getByRole('textbox', { name: 'Trou 1' })
    await userEvent.clear(blank)
    await userEvent.type(blank, 'main')
    await userEvent.click(within(check).getByRole('button', { name: 'Vérifier' }))

    expect(await within(check).findByRole('status')).toHaveTextContent('Notion validée !')
    expect(within(check).queryByRole('button', { name: 'Réessayer' })).not.toBeInTheDocument()
  })

  it('says when the notion is already validated', async () => {
    stubApi({
      ...SIGNED_IN_API,
      ...TOUR_API,
      '/api/v1/me/tour/go/hello-world': { body: checkpoint(true) },
    })
    renderAt('/subjects/go/tour/hello-world', signedIn())

    const check = await screen.findByRole('region', { name: 'Valider la notion' })
    expect(await within(check).findByRole('status')).toHaveTextContent('Notion validée !')
  })

  it('asks a visitor to sign in to validate the notion', async () => {
    stubApi(TOUR_API)
    const session = fakeSession()
    renderAt('/subjects/go/tour/hello-world', session)

    await userEvent.click(await screen.findByRole('button', { name: 'Se connecter pour valider' }))

    expect(session.login).toHaveBeenCalledWith('/subjects/go/tour/hello-world')
  })

  it('shows a not-found page for a notion outside the Tour', async () => {
    stubApi({
      '/api/v1/subjects/go/tour': { body: TEST_TOUR },
      '/api/v1/subjects/go/tour/inconnue': {
        status: 404,
        body: { status: 404, code: 'content.tour-notion-not-found' },
      },
    })
    renderAt('/subjects/go/tour/inconnue')

    expect(await screen.findByRole('heading', { name: 'Page introuvable' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Retour au sommaire du Tour' })).toHaveAttribute(
      'href',
      '/subjects/go/tour',
    )
  })
})
