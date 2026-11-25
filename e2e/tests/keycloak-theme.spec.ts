import { expect, test, type Page } from './support/test'
import { seriousViolations } from './support/axe'

/** Ouvre la page de connexion de Keycloak comme un visiteur : par le bouton « Se connecter » de l'application. */
async function openSignIn(page: Page) {
  await page.goto('/')
  await page.getByRole('button', { name: 'Se connecter' }).first().click()
  await expect(page.locator('#kc-form-login')).toBeVisible()
}

/**
 * Le thème de connexion (ADR 0075) : les pages servies par Keycloak ressemblent à Recallio, parlent français, ne
 * chargent rien d'un tiers et mènent aux pages légales (ADR 0074). Le thème n'a qu'un gabarit, le pied de page : ce
 * parcours est ce qui le revérifie à chaque montée de version de Keycloak.
 */
test.describe('Thème de connexion (ADR 0075)', () => {
  test('the sign-in page of the identity provider looks like Recallio, in French, without any third party', async ({
    page,
    baseURL,
  }) => {
    const origins = new Set<string>()
    page.on('request', (request) => {
      const url = new URL(request.url())
      if (url.protocol.startsWith('http')) origins.add(url.origin)
    })

    await openSignIn(page)

    await expect(page.locator('#kc-header-wrapper')).toHaveText('Recallio')
    await expect(page.getByRole('heading', { level: 1, name: 'Connecte-toi' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Se connecter' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Créer un compte' })).toBeVisible()
    await expect(page.locator('html')).toHaveAttribute('lang', 'fr')

    // La feuille du thème est appliquée, et Nunito est servie par Keycloak lui-même.
    await page.evaluate(() => document.fonts.ready)
    const title = page.getByRole('heading', { level: 1 })
    await expect(title).toHaveCSS('font-family', /Nunito Variable/)
    expect(await page.evaluate(() => document.fonts.check('16px "Nunito Variable"'))).toBe(true)
    expect([...origins].sort()).toEqual(
      [new URL(baseURL!).origin, new URL(page.url()).origin].sort(),
    )

    // Les deux liens du pied de page, vers l'origine publique que le conteneur a reçue (PUBLIC_ORIGIN).
    const legal = page.getByRole('navigation', { name: 'Informations légales' })
    await expect(legal.getByRole('link', { name: 'Mentions légales' })).toHaveAttribute(
      'href',
      `${new URL(baseURL!).origin}/mentions-legales`,
    )
    await expect(legal.getByRole('link', { name: 'Confidentialité' })).toHaveAttribute(
      'href',
      `${new URL(baseURL!).origin}/confidentialite`,
    )
  })

  test('a wrong password is said in French, next to the field', async ({ page }) => {
    await openSignIn(page)

    await page.locator('#username').fill('alice')
    await page.locator('#password').fill('pas-le-bon-mot-de-passe')
    await page.locator('#kc-login').click()

    await expect(page.locator('#input-error-username')).toHaveText(
      'Nom d’utilisateur ou mot de passe incorrect.',
    )
    await expect(page.locator('#username')).toHaveAttribute('aria-invalid', 'true')
  })

  test('the registration page leads to both legal pages', async ({ page }) => {
    await openSignIn(page)
    await page.getByRole('link', { name: 'Créer un compte' }).click()
    await expect(page.getByRole('heading', { level: 1, name: 'Crée ton compte' })).toBeVisible()
    const sentence = page.locator('#recallio-legal')
    await expect(sentence).toContainText('Avant de créer ton compte, ce que Recallio garde sur toi')

    await sentence.getByRole('link', { name: 'politique de confidentialité' }).click()
    await expect(
      page.getByRole('heading', { level: 1, name: 'Politique de confidentialité' }),
    ).toBeVisible()

    await page.goBack()
    await page.locator('#recallio-legal').getByRole('link', { name: 'mentions légales' }).click()
    await expect(page.getByRole('heading', { level: 1, name: 'Mentions légales' })).toBeVisible()
  })

  // Le thème sombre suit le réglage du système : la page de Keycloak ne connaît pas le choix fait dans le profil.
  for (const scheme of ['light', 'dark'] as const) {
    test(`the sign-in and registration pages have no serious accessibility violation, ${scheme} theme @mobile`, async ({
      page,
    }) => {
      await page.emulateMedia({ colorScheme: scheme })
      await openSignIn(page)
      expect(await seriousViolations(page)).toEqual([])

      await page.getByRole('link', { name: 'Créer un compte' }).click()
      await expect(page.locator('#kc-register-form')).toBeVisible()
      expect(await seriousViolations(page)).toEqual([])
      // Aucun défilement latéral, sur un téléphone comme sur un ordinateur.
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
        ),
      ).toBe(0)
    })
  }
})
