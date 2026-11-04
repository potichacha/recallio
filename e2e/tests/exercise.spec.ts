import { readdirSync, readFileSync } from 'node:fs'
import { join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'
import { expect, test } from '@playwright/test'
import { signIn, USERS } from './support/auth'

/** Les exercices du thème joué (content/java/exercices/objet), dont le test retrouve celui que la révision propose. */
const EXERCISES = fileURLToPath(new URL('../../content/java/exercices/objet', import.meta.url))

/** Une exécution passe par le vrai runner et un conteneur de bac à sable (ADR 0030) : quelques secondes. */
const RUN_TIMEOUT = 30_000

/** Le suffixe des onglets de test, en lecture seule (ExerciseEditor). */
const TEST_TAB = /\s*\(test\)$/

/** Les fichiers d'une partie d'un exercice (`tests`, `solution`...), par chemin relatif, packages compris. */
function filesOf(folder: string): Map<string, string> {
  const files = readdirSync(folder, { recursive: true, withFileTypes: true }).filter((entry) =>
    entry.isFile(),
  )
  return new Map(
    files.map((entry) => {
      const path = join(entry.parentPath, entry.name)
      return [relative(folder, path).replaceAll('\\', '/'), readFileSync(path, 'utf-8')]
    }),
  )
}

/** Le dossier de l'exercice dont les tests sont exactement ceux affichés. */
function solutionFor(testPaths: string[]): Map<string, string> {
  const expected = [...testPaths].sort().join()
  for (const notion of readdirSync(EXERCISES)) {
    for (const key of readdirSync(join(EXERCISES, notion))) {
      const folder = join(EXERCISES, notion, key)
      if ([...filesOf(join(folder, 'tests')).keys()].sort().join() === expected) {
        return filesOf(join(folder, 'solution'))
      }
    }
  }
  throw new Error(`Aucun exercice du thème n'a pour tests ${expected}`)
}

test.describe('Exercice de code', () => {
  test('runs the starter code against the tests, then submits a solution judged by them', async ({
    page,
  }) => {
    // Le thème est fixé, mais pas l'exercice : le test retrouve sa solution dans content/, quel que soit l'ordre des
    // nouvelles cartes (un nouvel exercice du thème peut passer devant).
    await signIn(page, USERS.alice, '/review?theme=java-objet&type=CODING_EXERCISE')
    const tabs = page.getByRole('tablist', { name: "Fichiers de l'exercice" }).getByRole('tab')
    await expect(tabs.first()).toBeVisible()
    const names = (await tabs.allTextContents()).map((name) => name.trim())
    const testPaths = names
      .filter((name) => TEST_TAB.test(name))
      .map((name) => name.replace(TEST_TAB, ''))
    const solution = solutionFor(testPaths)

    // Les tests se lisent dans leur propre onglet, sans pouvoir les modifier.
    await page.getByRole('tab', { name: testPaths[0] }).click()
    await expect(page.getByRole('textbox', { name: testPaths[0] })).toHaveAttribute(
      'contenteditable',
      'false',
    )

    await page.getByRole('button', { name: 'Lancer les tests' }).click()
    const results = page.getByRole('region', { name: 'Résultat des tests' })
    await expect(results).toContainText('Des tests échouent.', { timeout: RUN_TIMEOUT })

    for (const [path, content] of solution) {
      await page.getByRole('tab', { name: path, exact: true }).click()
      await page.getByRole('textbox', { name: path, exact: true }).fill(content)
    }
    await page.getByRole('button', { name: 'Rendre' }).click()

    await expect(page.getByRole('status')).toHaveText('Exercice réussi !', {
      timeout: RUN_TIMEOUT,
    })
    await expect(page.getByRole('region', { name: 'Solution' })).toBeVisible()
  })
})
