import { expect, test } from './support/test'
import { followSubjects, signIn, USERS } from './support/auth'
import { seriousViolations } from './support/axe'
import { answerCard } from './support/review'

/** Un entretien dans trois semaines : deux pour acquérir les notions essentielles, la dernière pour réviser. */
const THREE_WEEKS = 21

/** Le jour de l'entretien, dans le fuseau de la machine, celui du navigateur : `yyyy-mm-dd`. */
function inDays(days: number): string {
  const today = new Date()
  const day = new Date(today.getFullYear(), today.getMonth(), today.getDate() + days)
  const twoDigits = (value: number) => String(value).padStart(2, '0')
  return `${day.getFullYear()}-${twoDigits(day.getMonth() + 1)}-${twoDigits(day.getDate())}`
}

test.describe('Compte à rebours avant un entretien (ADR 0066)', () => {
  test('gives a date to Go, sees the countdown and the notions of the week, then removes the date', async ({
    page,
  }) => {
    await signIn(page, USERS.bob, '/profile')
    await followSubjects(page, ['Go'])
    // « Ta progression » ne montre la progression par sujet qu'après une première carte (ADR 0058, 0077).
    await page.goto('/review?subject=go')
    await page.getByRole('button', { name: 'Commencer' }).click()
    await answerCard(page)

    await page.goto('/progress')
    const interview = page.getByRole('group', { name: 'Entretien Go' })
    const give = interview.getByRole('button', { name: 'Tu as un entretien ? Donne sa date' })
    const remove = interview.getByRole('button', { name: 'Retirer la date' })
    // Une base déjà utilisée peut garder la date d'un parcours précédent : on repart sans date.
    await expect(give.or(remove)).toBeVisible()
    if (await remove.isVisible()) await remove.click()

    await give.click()
    // Exact : le formulaire s'appelle « Date de ton entretien en Go », et Playwright cherche sinon une sous-chaîne.
    await interview.getByLabel('Date de ton entretien', { exact: true }).fill(inDays(THREE_WEEKS))
    await interview.getByRole('button', { name: 'Valider' }).click()

    await expect(interview.getByText(/^Entretien dans \d+ jours$/)).toBeVisible()
    await expect(interview.getByText(/^(Dans les temps|En avance|En retard|Prêt)$/)).toBeVisible()
    await expect(interview.getByText(/^Cette semaine : \d+ notions?$/)).toBeVisible()
    const lesson = interview
      .getByRole('list', { name: 'Les notions de la semaine' })
      .getByRole('link')
      .first()
    await expect(lesson).toHaveAttribute('href', /^\/subjects\/go\/course\//)
    expect(await seriousViolations(page), 'le compte à rebours').toEqual([])

    await remove.click()
    await expect(give).toBeVisible()
  })
})
