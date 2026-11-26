/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import {
  fakeSession,
  renderAt,
  SIGNED_IN_API,
  stubApi,
  TEST_PROFILE,
  TEST_SUBJECTS,
} from '../test/render'
import {
  LEGAL_CONTACT_EMAIL_MARKER,
  LEGAL_CONTACT_EMAIL_TAG,
  LEGAL_PUBLISHER_MARKER,
  LEGAL_PUBLISHER_TAG,
  legalConfig,
} from './legalConfig'

const CONTACT = 'contact@recallio.example'

/** Pose une balise d'index.html, comme nginx l'aurait remplie au démarrage du conteneur. */
function serve(name: string, content: string) {
  const tag = document.createElement('meta')
  tag.name = name
  tag.content = content
  document.head.append(tag)
}

afterEach(() => {
  for (const name of [LEGAL_PUBLISHER_TAG, LEGAL_CONTACT_EMAIL_TAG]) {
    document.head.querySelector(`meta[name="${name}"]`)?.remove()
  }
})

describe('the publisher and the contact that nginx puts in index.html (ADR 0074)', () => {
  it('reads both values', () => {
    serve(LEGAL_PUBLISHER_TAG, 'Alice Martin')
    serve(LEGAL_CONTACT_EMAIL_TAG, CONTACT)

    expect(legalConfig()).toEqual({ publisher: 'Alice Martin', contactEmail: CONTACT })
  })

  it('knows no publisher when the name is empty, blank, missing or left as its marker', () => {
    serve(LEGAL_CONTACT_EMAIL_TAG, CONTACT)
    expect(legalConfig().publisher).toBeUndefined()

    for (const content of ['', '   ', LEGAL_PUBLISHER_MARKER]) {
      document.head.querySelector(`meta[name="${LEGAL_PUBLISHER_TAG}"]`)?.remove()
      serve(LEGAL_PUBLISHER_TAG, content)
      expect(legalConfig().publisher).toBeUndefined()
    }
  })

  it('knows no contact when the page did not go through nginx, as with npm run dev', () => {
    serve(LEGAL_CONTACT_EMAIL_TAG, LEGAL_CONTACT_EMAIL_MARKER)

    expect(legalConfig().contactEmail).toBeUndefined()
  })

  // Lus tels quels sur le disque : les marqueurs d'index.html sont ceux que nginx remplace, par des valeurs échappées.
  it('declares both tags in index.html, with the markers that nginx replaces', () => {
    const html = new DOMParser().parseFromString(readFileSync('index.html', 'utf8'), 'text/html')
    const template = readFileSync('nginx/default.conf.template', 'utf8')
    const content = (name: string) =>
      html.head.querySelector(`meta[name="${name}"]`)?.getAttribute('content')

    expect(content(LEGAL_PUBLISHER_TAG)).toBe(LEGAL_PUBLISHER_MARKER)
    expect(content(LEGAL_CONTACT_EMAIL_TAG)).toBe(LEGAL_CONTACT_EMAIL_MARKER)
    expect(template).toContain(`sub_filter '${LEGAL_PUBLISHER_MARKER}' '\${LEGAL_PUBLISHER_HTML}';`)
    expect(template).toContain(
      `sub_filter '${LEGAL_CONTACT_EMAIL_MARKER}' '\${LEGAL_CONTACT_EMAIL_HTML}';`,
    )
  })
})

describe('the legal notice (ADR 0074)', () => {
  it('opens without an account and names the publisher, the host and the contact', async () => {
    serve(LEGAL_PUBLISHER_TAG, 'Alice Martin')
    serve(LEGAL_CONTACT_EMAIL_TAG, CONTACT)
    stubApi({})
    renderAt('/mentions-legales')

    expect(await screen.findByRole('heading', { level: 1, name: 'Mentions légales' })).toBeVisible()
    expect(screen.getByText('Alice Martin')).toBeInTheDocument()
    expect(screen.getByText(/OVH SAS, 2 rue Kellermann, 59100 Roubaix, France/)).toBeInTheDocument()
    for (const link of screen.getAllByRole('link', { name: CONTACT })) {
      expect(link).toHaveAttribute('href', `mailto:${CONTACT}`)
    }
    expect(screen.getAllByRole('link', { name: CONTACT }).length).toBeGreaterThan(0)
    expect(screen.queryByText(/ne publie pas son nom/)).not.toBeInTheDocument()
    expect(document.title).toBe('Mentions légales · Recallio')
  })

  it('says that the publisher is known to the host only when no name is given', async () => {
    serve(LEGAL_PUBLISHER_TAG, '')
    serve(LEGAL_CONTACT_EMAIL_TAG, CONTACT)
    stubApi({})
    renderAt('/mentions-legales')

    expect(
      await screen.findByText(/édité par un particulier, à titre non professionnel/),
    ).toBeVisible()
    expect(
      screen.getByText(/il a communiqué son identité à l’hébergeur du site.*article 1-1, II/),
    ).toBeInTheDocument()
  })

  // nginx écrit le nom échappé (&#39;, &quot;, &amp;) : le navigateur rend les caractères, et React les affiche en texte.
  it('shows a name with an apostrophe, a quote or a tag as plain text', async () => {
    const html = new DOMParser().parseFromString(
      '<meta content="Jeanne d&#39;Arc &amp; &quot;Fils&quot; &lt;b&gt;">',
      'text/html',
    )
    const name = html.querySelector('meta')?.content ?? ''
    serve(LEGAL_PUBLISHER_TAG, name)
    serve(LEGAL_CONTACT_EMAIL_TAG, CONTACT)
    stubApi({})
    renderAt('/mentions-legales')

    expect(await screen.findByText('Jeanne d\'Arc & "Fils" <b>')).toBeVisible()
  })

  it('does not invent a contact when none is configured', async () => {
    stubApi({})
    renderAt('/mentions-legales')

    expect(await screen.findAllByText(/non configurée sur cette installation/)).not.toHaveLength(0)
    expect(screen.queryByRole('link', { name: /@/ })).not.toBeInTheDocument()
  })
})

describe('the privacy policy (ADR 0074)', () => {
  it('opens without an account, with its date, its contact and what is kept', async () => {
    serve(LEGAL_CONTACT_EMAIL_TAG, CONTACT)
    stubApi({})
    renderAt('/confidentialite')

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Politique de confidentialité' }),
    ).toBeVisible()
    expect(screen.getByText(/^Dernière mise à jour : \d{1,2} \S+ \d{4}\.$/)).toBeInTheDocument()
    expect(screen.getByText(/L’éditeur de Recallio, un particulier/)).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: CONTACT })[0]).toHaveAttribute(
      'href',
      `mailto:${CONTACT}`,
    )
    for (const caption of [
      'Sans compte',
      'Avec un compte',
      'Sur ton appareil',
      'Les prestataires',
    ]) {
      expect(screen.getByRole('table', { name: caption })).toBeInTheDocument()
    }
    expect(screen.getByRole('link', { name: 'www.cnil.fr' })).toHaveAttribute(
      'href',
      'https://www.cnil.fr',
    )
    expect(document.title).toBe('Politique de confidentialité · Recallio')
  })

  // La page Bêta des admins (ADR 0076) : des chiffres d'ensemble tirés de l'activité des comptes, dits ici.
  it('says that the publisher draws overall figures from the activity, never a record per person', async () => {
    stubApi({})
    renderAt('/confidentialite')

    const progress = await screen.findByRole('rowheader', { name: 'Ta progression' })
    expect(progress.closest('tr')).toHaveTextContent(
      /L’éditeur en tire aussi des chiffres d’ensemble .*, sans fiche par personne, pour améliorer le contenu\./,
    )
    const basis = screen.getByText('L’intérêt légitime de l’éditeur').closest('li')
    expect(basis).toHaveTextContent(
      /les chiffres d’ensemble tirés de l’activité des comptes \(combien révisent, quelles cartes sont le plus ratées\), sans fiche par personne\./,
    )
  })

  // La révision intensive (ADR 0078) ne garde qu'un compteur par jour : ni la carte, ni la réponse.
  it('says that the intensive drill keeps a daily counter and not the answers', async () => {
    stubApi({})
    renderAt('/confidentialite')

    const progress = await screen.findByRole('rowheader', { name: 'Ta progression' })
    expect(progress.closest('tr')).toHaveTextContent(
      /En révision intensive, tes réponses sont corrigées sans être enregistrées : seul un compteur est gardé, le nombre de réponses de chaque jour et combien étaient justes\./,
    )
    // L'idempotence de la correction (ADR 0079) : une empreinte, où ni la carte ni la réponse ne se lisent.
    expect(progress.closest('tr')).toHaveTextContent(
      /une empreinte de tes dernières réponses du jour, pour ne pas en compter une deux fois : ni la carte ni ta réponse ne s’y lisent\./,
    )
  })

  // Le droit « Plus » et les codes d'accès (ADR 0079) : le droit, le code utilisé, jamais le code lui-même.
  it('says what Recallio Plus keeps: the right, the code used and when, never the code itself', async () => {
    stubApi({})
    renderAt('/confidentialite')

    const plus = await screen.findByRole('rowheader', { name: 'Recallio Plus' })
    expect(plus.closest('tr')).toHaveTextContent(
      /la date de fin de ton droit, quel code d’accès tu as utilisé et quand, et le nombre de codes que tu as essayés sans succès, avec l’heure du premier/,
    )
    expect(plus.closest('tr')).toHaveTextContent(
      /Le code lui-même n’est pas gardé : seulement une empreinte, et ses quatre derniers caractères\./,
    )
  })

  it('names the publisher as the one responsible when a name is given', async () => {
    serve(LEGAL_PUBLISHER_TAG, 'Alice Martin')
    serve(LEGAL_CONTACT_EMAIL_TAG, CONTACT)
    stubApi({})
    renderAt('/confidentialite')

    expect(await screen.findByText('Alice Martin')).toBeVisible()
  })

  // Un tableau qui ne tient pas défile dans sa zone, que le clavier atteint : la page ne défile pas de côté.
  it('keeps each table in a region that the keyboard reaches, named after the table', async () => {
    stubApi({})
    renderAt('/confidentialite')

    const region = await screen.findByRole('region', { name: 'Avec un compte' })
    expect(region).toHaveAttribute('tabindex', '0')
    expect(within(region).getByRole('rowheader', { name: 'Ton identité' })).toBeInTheDocument()
  })
})

describe('the links to the legal pages (ADR 0074)', () => {
  const legalLinks = (container: HTMLElement) => ({
    privacy: within(container).getByRole('link', { name: /confidentialité/i }),
    notice: within(container).getByRole('link', { name: /mentions légales/i }),
  })

  it('offers both pages in the footer of every page, to a visitor', async () => {
    stubApi({ '/api/v1/subjects': { body: [] } })
    renderAt('/subjects')

    const footer = within(await screen.findByRole('contentinfo')).getByRole('navigation', {
      name: 'Informations légales',
    })
    expect(legalLinks(footer).notice).toHaveAttribute('href', '/mentions-legales')
    expect(legalLinks(footer).privacy).toHaveAttribute('href', '/confidentialite')
  })

  it('offers both pages in the footer to a signed-in user', async () => {
    stubApi({ ...SIGNED_IN_API, '/api/v1/subjects': { body: [] } })
    renderAt('/subjects', fakeSession({ status: 'authenticated', userName: 'Alice' }))

    const footer = await screen.findByRole('contentinfo')
    expect(legalLinks(footer).notice).toBeInTheDocument()
    expect(legalLinks(footer).privacy).toBeInTheDocument()
  })

  it('tells where to read them on the public page, next to the account creation', async () => {
    stubApi({
      '/api/v1/subjects': { body: TEST_SUBJECTS },
      '/api/v1/subjects/java/trial': { body: { questions: [] } },
      'POST /api/v1/funnel/visits': { status: 204, body: null },
    })
    renderAt('/')

    const start = (await screen.findByRole('heading', { name: 'On commence ?' })).closest('section')
    expect(start).not.toBeNull()
    const links = legalLinks(start as HTMLElement)
    expect(links.privacy).toHaveAttribute('href', '/confidentialite')
    expect(links.notice).toHaveAttribute('href', '/mentions-legales')
  })

  it('tells where to read them at the first sign-in', async () => {
    stubApi({
      ...SIGNED_IN_API,
      '/api/v1/me': { body: { ...TEST_PROFILE, followedSubjects: [], tracks: {} } },
      '/api/v1/subjects': { body: TEST_SUBJECTS },
    })
    renderAt('/welcome', fakeSession({ status: 'authenticated', userName: 'Alice' }))

    await screen.findByRole('heading', { name: 'Qu’est-ce qui t’amène ?' })
    const links = legalLinks(screen.getByRole('main'))
    expect(links.privacy).toHaveAttribute('href', '/confidentialite')
    expect(links.notice).toHaveAttribute('href', '/mentions-legales')
  })

  it('leads from « Mes données », in the profile, to the privacy policy', async () => {
    stubApi({ ...SIGNED_IN_API, '/api/v1/subjects': { body: TEST_SUBJECTS } })
    renderAt('/profile/data', fakeSession({ status: 'authenticated', userName: 'Alice' }))

    const section = (await screen.findByRole('heading', { name: 'Mes données', level: 2 })).closest(
      'section',
    )
    expect(
      within(section as HTMLElement).getByRole('link', { name: 'politique de confidentialité' }),
    ).toHaveAttribute('href', '/confidentialite')
  })
})
