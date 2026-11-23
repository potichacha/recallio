/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { describe, expect, it, vi } from 'vitest'
import { monitoringConfig, SENTRY_TAGS } from './config'
import { createMonitoring, startMonitoring, type LoadSentry } from './monitoring'

const { sentryModule } = vi.hoisted(() => ({ sentryModule: vi.fn() }))
vi.mock('./sentry', () => {
  sentryModule()
  return { startSentry: vi.fn() }
})

const CONFIG = { dsn: 'https://public@o1.ingest.de.sentry.io/2', environment: 'production' }

/** Un Sentry factice : ce qu'il a reçu, et de quoi finir son téléchargement quand le test le décide. */
function fakeSentry() {
  const captured: unknown[] = []
  let finishDownload: () => void = () => {}
  const downloaded = new Promise<void>((resolve) => (finishDownload = resolve))
  const load = vi.fn<LoadSentry>(async () => {
    await downloaded
    return () => (error: unknown) => captured.push(error)
  })
  return { load, captured, finishDownload }
}

describe('the error tracking of the PWA (ADR 0070)', () => {
  it('downloads nothing without a DSN', async () => {
    const sentry = fakeSentry()
    const monitoring = createMonitoring(sentry.load)

    await monitoring.start(undefined)
    monitoring.report(new Error('Une page qui casse'))

    expect(sentry.load).not.toHaveBeenCalled()
    expect(sentry.captured).toEqual([])
  })

  it('never imports Sentry when index.html gives no DSN, as with npm run dev', async () => {
    await startMonitoring()

    expect(sentryModule).not.toHaveBeenCalled()
  })

  it('sends the errors reported while Sentry was downloading, once it is there', async () => {
    const sentry = fakeSentry()
    const monitoring = createMonitoring(sentry.load)
    const early = new Error('Avant le téléchargement')

    const started = monitoring.start(CONFIG)
    monitoring.report(early)
    expect(sentry.captured).toEqual([])
    sentry.finishDownload()
    await started
    const late = new Error('Après le téléchargement')
    monitoring.report(late)

    expect(sentry.load).toHaveBeenCalledTimes(1)
    expect(sentry.captured).toEqual([early, late])
  })

  it('reports the same error only once', async () => {
    const sentry = fakeSentry()
    sentry.finishDownload()
    const monitoring = createMonitoring(sentry.load)
    await monitoring.start(CONFIG)
    const error = new Error('Une page rendue deux fois')

    monitoring.report(error)
    monitoring.report(error)

    expect(sentry.captured).toEqual([error])
  })

  it('keeps the PWA running when Sentry cannot be downloaded', async () => {
    const monitoring = createMonitoring(() => Promise.reject(new Error('Hors ligne')))

    await expect(monitoring.start(CONFIG)).resolves.toBeUndefined()
    expect(() => monitoring.report(new Error('Ensuite'))).not.toThrow()
  })
})

describe('the configuration of Sentry, filled in by nginx (ADR 0070)', () => {
  const tags = (values: Record<string, string>) => (name: string) => values[name]

  it('reads the DSN, the environment and the release', () => {
    expect(
      monitoringConfig(
        tags({
          [SENTRY_TAGS.dsn.name]: CONFIG.dsn,
          [SENTRY_TAGS.environment.name]: 'production',
          [SENTRY_TAGS.release.name]: 'sha-0123456789ab',
        }),
      ),
    ).toEqual({ dsn: CONFIG.dsn, environment: 'production', release: 'sha-0123456789ab' })
  })

  it('turns Sentry off when the DSN is empty, as nginx serves it by default', () => {
    expect(monitoringConfig(tags({ [SENTRY_TAGS.dsn.name]: '' }))).toBeUndefined()
  })

  it('turns Sentry off when the tags keep their markers, as with npm run dev', () => {
    expect(
      monitoringConfig(tags({ [SENTRY_TAGS.dsn.name]: SENTRY_TAGS.dsn.marker })),
    ).toBeUndefined()
  })

  it('leaves out an empty release', () => {
    expect(
      monitoringConfig(
        tags({ [SENTRY_TAGS.dsn.name]: CONFIG.dsn, [SENTRY_TAGS.release.name]: '' }),
      ),
    ).toEqual({ dsn: CONFIG.dsn, environment: undefined, release: undefined })
  })

  it('finds in index.html the tags that nginx fills in, with their markers', () => {
    const html = new DOMParser().parseFromString(readFileSync('index.html', 'utf8'), 'text/html')

    for (const { name, marker } of Object.values(SENTRY_TAGS)) {
      expect(html.head.querySelector(`meta[name="${name}"]`)?.getAttribute('content')).toBe(marker)
    }
  })
})
