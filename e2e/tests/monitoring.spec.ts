import { expect, test } from './support/test'

/**
 * Le suivi des erreurs (ADR 0070) : nginx met le DSN de Sentry dans index.html. Le compose n'en donne pas : la PWA ne
 * doit alors rien télécharger de Sentry, ni rien lui envoyer.
 */
test.describe('Suivi des erreurs (ADR 0070)', () => {
  test('serves the Sentry tags filled in by nginx, empty without a DSN', async ({ request }) => {
    const html = await (await request.get('/')).text()

    expect(html).toContain('<meta name="recallio-sentry-dsn" content="" />')
    expect(html).toContain('<meta name="recallio-sentry-environment" content="production" />')
    expect(html).toContain('<meta name="recallio-sentry-release" content="" />')
  })

  test('downloads nothing from Sentry and sends it nothing, without a DSN', async ({ page }) => {
    const requests: string[] = []
    page.on('request', (sent) => requests.push(sent.url()))

    await page.goto('/')
    await expect(
      page.getByRole('heading', {
        name: 'Prêt pour ton entretien technique, dix minutes par jour.',
      }),
    ).toBeVisible()
    await page.waitForLoadState('networkidle')

    expect(requests.filter((url) => /sentry/i.test(url))).toEqual([])
  })
})
