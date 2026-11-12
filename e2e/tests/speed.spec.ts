import { appendFileSync } from 'node:fs'
import { expect, test, type Page } from './support/test'

/**
 * La vitesse ressentie (ADR 0058), mesurée à chaque pull request sur un téléphone lent simulé : processeur quatre fois
 * plus lent, réseau d'une 4G moyenne, cache vide. Le budget du JavaScript (`npm run size`) borne ce qu'on télécharge ;
 * ces mesures disent ce que l'utilisateur attend réellement.
 */
const CPU_SLOWDOWN = 4
/** Une 4G moyenne : 1,6 Mbit/s en descente, 750 kbit/s en montée, 150 ms de latence. */
const NETWORK = {
  offline: false,
  downloadThroughput: (1.6 * 1024 * 1024) / 8,
  uploadThroughput: (750 * 1024) / 8,
  latency: 150,
}

/** Les seuils « bon » de Google pour le plus grand affichage et la stabilité de la page, avec la marge de la CI. */
const MAX_LCP_MS = 4000
const MAX_CLS = 0.1

type Vitals = { lcp: number; cls: number; transferKb: number; requests: number }

/** Ouvre la page à froid et rend ce que le navigateur a mesuré, une fois la page stable. */
async function measure(page: Page, path: string): Promise<Vitals> {
  const session = await page.context().newCDPSession(page)
  await session.send('Network.enable')
  await session.send('Network.setCacheDisabled', { cacheDisabled: true })
  await session.send('Network.emulateNetworkConditions', NETWORK)
  await session.send('Emulation.setCPUThrottlingRate', { rate: CPU_SLOWDOWN })

  await page.goto(path)
  await expect(page.locator('main h1').first()).toBeVisible()
  await expect(page.locator('main [aria-busy="true"]')).toHaveCount(0)

  const vitals = await page.evaluate(
    () =>
      new Promise<Vitals>((resolve) => {
        let lcp = 0
        let cls = 0
        new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) lcp = Math.max(lcp, entry.startTime)
        }).observe({ type: 'largest-contentful-paint', buffered: true })
        new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            const shift = entry as PerformanceEntry & { value: number; hadRecentInput: boolean }
            if (!shift.hadRecentInput) cls += shift.value
          }
        }).observe({ type: 'layout-shift', buffered: true })
        // Les observateurs livrent d'abord ce qu'ils ont déjà vu : on lit au tour suivant.
        setTimeout(() => {
          const resources = performance.getEntriesByType('resource') as PerformanceResourceTiming[]
          resolve({
            lcp,
            cls,
            transferKb: resources.reduce((sum, { transferSize }) => sum + transferSize, 0) / 1024,
            requests: resources.length,
          })
        }, 0)
      }),
  )
  await session.detach()
  return vitals
}

/** Publie les mesures dans le résumé du job, en CI : on voit la vitesse de chaque pull request sans ouvrir un rapport. */
function publish(rows: [string, Vitals][]) {
  const summary = process.env.GITHUB_STEP_SUMMARY
  if (!summary) return
  const lines = [
    '## Vitesse sur un téléphone lent simulé',
    '',
    '| Page | Plus grand affichage | Stabilité (CLS) | Téléchargé | Requêtes |',
    '|---|---:|---:|---:|---:|',
    ...rows.map(
      ([path, { lcp, cls, transferKb, requests }]) =>
        `| \`${path}\` | ${(lcp / 1000).toFixed(2)} s | ${cls.toFixed(3)} | ${transferKb.toFixed(0)} Ko | ${requests} |`,
    ),
    '',
  ]
  appendFileSync(summary, lines.join('\n'))
}

test.describe('Vitesse (ADR 0058)', () => {
  test('shows the public pages quickly, without the layout jumping, on a slow phone', async ({
    page,
  }) => {
    test.slow()
    const rows: [string, Vitals][] = []
    for (const path of ['/', '/learn', '/try/go']) {
      rows.push([path, await measure(page, path)])
    }
    publish(rows)

    for (const [path, { lcp, cls }] of rows) {
      console.log(
        `${path} : plus grand affichage ${(lcp / 1000).toFixed(2)} s, CLS ${cls.toFixed(3)}`,
      )
      expect(lcp, `plus grand affichage de ${path}`).toBeLessThan(MAX_LCP_MS)
      expect(cls, `stabilité de ${path}`).toBeLessThan(MAX_CLS)
    }
  })
})
