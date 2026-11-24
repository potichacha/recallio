import { expect, test } from './support/test'

/**
 * Les valeurs que le compose donne au front (LEGAL_PUBLISHER, LEGAL_CONTACT_EMAIL, infra/compose.yaml). Le nom porte
 * une apostrophe exprès : nginx l'écrit dans index.html, et ne doit ni casser sa configuration ni la page.
 */
const PUBLISHER = "L'équipe de développement"
const CONTACT = 'contact@recallio.example'

/**
 * Les pages légales (ADR 0074), sur la pile comme en production : lisibles sans compte, avec l'éditeur et le contact
 * que le conteneur a reçus à son démarrage, et liées depuis le pied de chaque page.
 */
test.describe('Pages légales (ADR 0074)', () => {
  test('a visitor reads the legal notice, with the publisher and the contact given to the container', async ({
    page,
  }) => {
    await page.goto('/mentions-legales')

    await expect(page.getByRole('heading', { level: 1, name: 'Mentions légales' })).toBeVisible()
    await expect(page).toHaveTitle('Mentions légales · Recallio')
    const main = page.locator('main')
    await expect(main.getByText(PUBLISHER, { exact: true })).toBeVisible()
    await expect(main.getByText(/OVH SAS, 2 rue Kellermann, 59100 Roubaix, France/)).toBeVisible()
    await expect(main.getByRole('link', { name: CONTACT }).first()).toHaveAttribute(
      'href',
      `mailto:${CONTACT}`,
    )
  })

  test('the footer leads from one legal page to the other', async ({ page }) => {
    await page.goto('/mentions-legales')
    const footer = page.getByRole('contentinfo')

    await footer.getByRole('link', { name: 'Confidentialité' }).click()

    await expect(
      page.getByRole('heading', { level: 1, name: 'Politique de confidentialité' }),
    ).toBeVisible()
    await expect(page).toHaveTitle('Politique de confidentialité · Recallio')
    await expect(page.locator('main').getByRole('link', { name: CONTACT }).first()).toBeVisible()
    await expect(page.getByRole('table', { name: 'Avec un compte' })).toBeVisible()

    await footer.getByRole('link', { name: 'Mentions légales' }).click()
    await expect(page.getByRole('heading', { level: 1, name: 'Mentions légales' })).toBeVisible()
  })

  // Sur un téléphone, la barre de navigation est fixée en bas de l'écran : en bas de page, elle ne couvre pas le pied
  // de page. Sur un ordinateur, elle est en haut : le parcours y vaut aussi.
  test('the navigation bar never covers the footer, at the bottom of a page @mobile', async ({
    page,
  }) => {
    await page.goto('/confidentialite')
    const link = page.getByRole('contentinfo').getByRole('link', { name: 'Mentions légales' })
    await expect(link).toBeVisible()

    await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight))
    await expect(link).toBeInViewport({ ratio: 1 })

    const navigation = await page
      .getByRole('navigation', { name: 'Navigation principale' })
      .boundingBox()
    const footer = await link.boundingBox()
    expect(navigation).not.toBeNull()
    expect(footer).not.toBeNull()
    if (!navigation || !footer) return
    const apart =
      footer.y + footer.height <= navigation.y || navigation.y + navigation.height <= footer.y
    expect(apart, 'le lien du pied de page et la barre de navigation ne se recouvrent pas').toBe(
      true,
    )
  })

  test('nginx escapes the publisher it writes in index.html', async ({ request }) => {
    const html = await (await request.get('/mentions-legales')).text()

    expect(html).toContain(
      '<meta name="recallio-legal-publisher" content="L&#39;équipe de développement"',
    )
    expect(html).toContain(`<meta name="recallio-legal-contact-email" content="${CONTACT}"`)
    expect(html).not.toContain('__LEGAL_')
  })
})
