import { expect, test } from './support/test'

/** Le débit permis à l'essai sans compte est de 2 requêtes par seconde, avec une rafale de 10 (ADR 0054). */
const BURST = 30

test.describe('Sécurité (ADR 0054)', () => {
  test('sends a Content-Security-Policy with the pages and with the API', async ({ request }) => {
    for (const path of ['/', '/review', '/api/v1/subjects']) {
      const policy = (await request.get(path)).headers()['content-security-policy']
      expect(policy, path).toContain("default-src 'self'")
      expect(policy, path).toContain("script-src 'self';")
      expect(policy, path).toContain("frame-ancestors 'none'")
    }
  })

  test('refuses a burst of anonymous requests on the trial, then accepts them again', async ({
    request,
  }) => {
    const responses = await Promise.all(
      Array.from({ length: BURST }, () => request.get('/api/v1/subjects/go/trial')),
    )

    const refused = responses.filter((response) => response.status() === 429)
    expect(refused.length).toBeGreaterThan(0)
    expect(responses.some((response) => response.status() === 200)).toBe(true)
    // Un Problem Details, comme une erreur du backend : le front affiche son détail.
    expect(refused[0].headers()['content-type']).toContain('application/problem+json')
    expect(await refused[0].json()).toMatchObject({
      status: 429,
      code: 'too-many-requests',
      detail: 'Trop de demandes en peu de temps. Réessaie dans un instant.',
    })

    // La limite se relâche d'elle-même : l'essai des parcours suivants n'est pas gêné.
    await expect
      .poll(async () => (await request.get('/api/v1/subjects/go/trial')).status(), {
        timeout: 15_000,
      })
      .toBe(200)
  })
})
