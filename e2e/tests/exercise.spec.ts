import { expect, test } from '@playwright/test'
import { signIn, USERS } from './support/auth'

/** La solution de l'exercice « point-egal » (content/java/exercices/objet/hashcode-contrat/point-egal). */
const SOLUTION = `import java.util.Objects;

public final class Point {
    private final int x;
    private final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() { return x; }

    public int y() { return y; }

    @Override
    public boolean equals(Object other) {
        return other instanceof Point point && x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
`

/** Une exécution passe par le vrai runner et un conteneur de bac à sable (ADR 0030) : quelques secondes. */
const RUN_TIMEOUT = 30_000

test.describe('Exercice de code', () => {
  test('runs the starter code against the tests, then submits a solution judged by them', async ({
    page,
  }) => {
    await signIn(page, USERS.alice, '/review?subject=java&type=CODING_EXERCISE')
    const editor = page.getByRole('textbox', { name: 'Point.java' })
    await expect(editor).toBeVisible()

    await page.getByRole('button', { name: 'Lancer les tests' }).click()
    const results = page.getByRole('region', { name: 'Résultat des tests' })
    await expect(results).toContainText('Des tests échouent.', { timeout: RUN_TIMEOUT })
    await expect(results).toContainText('deuxPointsDeMemesCoordonneesSontEgaux')

    await editor.fill(SOLUTION)
    await page.getByRole('button', { name: 'Rendre' }).click()

    await expect(page.getByRole('status')).toHaveText('Exercice réussi !', {
      timeout: RUN_TIMEOUT,
    })
    await expect(page.getByRole('region', { name: 'Solution' })).toContainText('Objects.hash')
  })
})
