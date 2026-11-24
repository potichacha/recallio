import { expect, test } from './support/test'

/**
 * L'origine publique que le compose donne au front et au backend (PUBLIC_ORIGIN), quelle que soit l'adresse où les
 * parcours joignent la pile.
 */
const PUBLIC_ORIGIN = 'http://localhost:3000'

/**
 * Le référencement (ADR 0065), sur la pile comme en production : nginx met l'origine publique dans robots.txt et dans
 * les balises de partage, sert l'image de partage, et relaie le plan du site vers le backend. Des requêtes, sans page.
 */
test.describe('Référencement (ADR 0065)', () => {
  test('points the crawlers to the sitemap, at the public origin', async ({ request }) => {
    const response = await request.get('/robots.txt')

    expect(response.status()).toBe(200)
    expect(await response.text()).toContain(`Sitemap: ${PUBLIC_ORIGIN}/sitemap.xml`)
  })

  test('serves the sitemap of the backend, with the pages of the Go Tour', async ({ request }) => {
    const response = await request.get('/sitemap.xml')

    expect(response.status()).toBe(200)
    expect(response.headers()['content-type']).toContain('application/xml')
    const sitemap = await response.text()
    expect(sitemap).toContain('http://www.sitemaps.org/schemas/sitemap/0.9')
    expect(sitemap).toContain(`<loc>${PUBLIC_ORIGIN}/subjects/go/tour/hello-world</loc>`)
    // Les deux pages légales (ADR 0074).
    expect(sitemap).toContain(`<loc>${PUBLIC_ORIGIN}/mentions-legales</loc>`)
    expect(sitemap).toContain(`<loc>${PUBLIC_ORIGIN}/confidentialite</loc>`)
  })

  test('gives the pages their share tags, with an absolute image', async ({ request }) => {
    const page = await (await request.get('/')).text()

    expect(page).toContain(`content="${PUBLIC_ORIGIN}/og-image.png"`)
    expect(page).not.toContain('__PUBLIC_ORIGIN__')
  })

  test('serves the share image', async ({ request }) => {
    const response = await request.get('/og-image.png')

    expect(response.status()).toBe(200)
    expect(response.headers()['content-type']).toBe('image/png')
  })
})
