import { expect, test } from './support/test'
import { followSubjects, signIn, USERS } from './support/auth'

test.describe('Navigation (ADR 0077)', () => {
  test('goes through the four destinations, and opens the profile from the avatar @mobile', async ({
    page,
  }) => {
    await signIn(page, USERS.bob, '/profile')
    await followSubjects(page, ['Java'])
    const navigation = page.getByRole('navigation', { name: 'Navigation principale' })
    const destination = (name: string) => navigation.getByRole('link', { name })
    await expect(navigation.getByRole('link')).toHaveText([
      'Accueil',
      'Apprendre',
      'S’entraîner',
      'Jouer',
    ])

    // L'accueil : Rico salue, et la carte dominante reprend la leçon du sujet en cours.
    await destination('Accueil').click()
    await expect(
      page.getByRole('heading', { level: 1, name: /^(Salut|Bienvenue) Bob$/ }),
    ).toBeVisible()
    await expect(destination('Accueil')).toHaveAttribute('aria-current', 'page')
    await expect(page.getByRole('link', { name: /^(Continuer|Commencer)$/ })).toBeVisible()

    // Apprendre : les révisions du jour, puis les sujets. La session de révision reste sous cette porte.
    await destination('Apprendre').click()
    await expect(
      page.getByRole('heading', { level: 1, name: 'Tes sujets, pas à pas' }),
    ).toBeVisible()
    await page.getByRole('link', { name: /^Révisions du jour/ }).click()
    await expect(page).toHaveURL(/\/review$/)
    await expect(destination('Apprendre')).toHaveAttribute('aria-current', 'page')

    // S'entraîner : l'examen blanc et le carnet d'erreurs de chaque sujet suivi.
    await destination('S’entraîner').click()
    await expect(page.getByRole('heading', { level: 1, name: 'Comme en entretien' })).toBeVisible()
    const java = page.getByRole('region', { name: 'Java' })
    await expect(java.getByRole('link', { name: /^Carnet d’erreurs/ })).toBeVisible()
    await java.getByRole('link', { name: /^Examen blanc/ }).click()
    await expect(page.getByRole('heading', { name: 'Examen blanc : Java' })).toBeVisible()
    await expect(destination('S’entraîner')).toHaveAttribute('aria-current', 'page')

    // Jouer : le défi du jour, le classé et les groupes.
    await destination('Jouer').click()
    await expect(
      page.getByRole('heading', { level: 1, name: 'Mesure-toi aux autres' }),
    ).toBeVisible()
    await expect(page.getByRole('link', { name: /^Classé/ })).toBeVisible()
    await expect(page.getByRole('region', { name: 'Défi du jour' })).toBeVisible()

    // Le profil n'est pas une destination : il s'ouvre par l'avatar.
    await page.getByRole('link', { name: USERS.bob.displayName }).click()
    await expect(page).toHaveURL(/\/profile$/)
    await expect(navigation.locator('[aria-current]')).toHaveCount(0)
  })

  test('shows a visitor the same destinations, and asks to sign in where an account is needed @mobile', async ({
    page,
  }) => {
    await page.goto('/learn')
    const navigation = page.getByRole('navigation', { name: 'Navigation principale' })
    await expect(navigation.getByRole('link')).toHaveText([
      'Accueil',
      'Apprendre',
      'S’entraîner',
      'Jouer',
    ])

    await navigation.getByRole('link', { name: 'S’entraîner' }).click()
    await expect(page.getByRole('heading', { name: 'Connexion requise' })).toBeVisible()

    await navigation.getByRole('link', { name: 'Jouer' }).click()
    await expect(page.getByRole('button', { name: 'Connecte-toi pour jouer' })).toBeVisible()
  })
})
