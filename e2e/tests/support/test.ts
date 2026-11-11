import { expect, test as base, type BrowserContext } from '@playwright/test'

export { expect }
export type { Locator, Page } from '@playwright/test'

/**
 * Le `test` de tous les parcours : il échoue si le navigateur signale une violation de la Content-Security-Policy
 * (ADR 0054), dans la page du test comme dans celles qu'il ouvre lui-même (`browser.newContext()`). Une politique trop
 * stricte casserait une fonction sans bruit ; ici, elle casse un parcours.
 */
export const test = base.extend<{ cspViolations: string[] }>({
  cspViolations: [
    async ({ browser, context }, use) => {
      const violations: string[] = []
      const watch = (watched: BrowserContext) =>
        watched.on('console', (message) => {
          if (message.text().includes('Content Security Policy')) violations.push(message.text())
        })
      watch(context)
      const newContext = browser.newContext.bind(browser)
      browser.newContext = async (options) => {
        const created = await newContext(options)
        watch(created)
        return created
      }

      await use(violations)

      browser.newContext = newContext
      expect(violations, 'violations de la Content-Security-Policy').toEqual([])
    },
    { auto: true },
  ],
})
