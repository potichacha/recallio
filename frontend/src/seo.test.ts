/// <reference types="node" />
import { readFileSync } from 'node:fs'
import type { RouteObject } from 'react-router'
import { describe, expect, it } from 'vitest'
import robots from '../public/robots.txt?raw'
import { accountPages, pages } from './routes'

/**
 * Le référencement (ADR 0065) : ce que robots.txt laisse aux moteurs de recherche, et les balises que lit l'aperçu d'un
 * lien partagé. Les deux fichiers sont lus tels quels, avant que nginx n'y mette l'origine publique du site.
 */

/** Une règle de robots.txt, lue comme Google et Bing (RFC 9309) : `*` vaut n'importe quoi, `$` ancre la fin. */
type Rule = { allow: boolean; path: string; pattern: RegExp }

function rules(): Rule[] {
  return robots
    .split('\n')
    .map((line) => line.replace(/#.*/, '').trim())
    .map((line) => /^(Allow|Disallow):\s*(\S+)$/i.exec(line))
    .filter((match) => match !== null)
    .map(([, kind, path]) => ({
      allow: kind.toLowerCase() === 'allow',
      path,
      pattern: pattern(path),
    }))
}

function pattern(path: string): RegExp {
  const anchored = path.endsWith('$')
  const body = (anchored ? path.slice(0, -1) : path)
    .split('*')
    .map((part) => part.replace(/[.+?^${}()|[\]\\]/g, '\\$&'))
    .join('.*')
  return new RegExp(`^${body}${anchored ? '$' : ''}`)
}

/** La règle la plus longue qui s'applique décide ; à égalité, Allow l'emporte. Sans règle, l'adresse est permise. */
function isBlocked(path: string): boolean {
  const applying = rules()
    .filter((rule) => rule.pattern.test(path))
    .sort((a, b) => b.path.length - a.path.length || Number(b.allow) - Number(a.allow))
  return applying.length > 0 && !applying[0].allow
}

/** Une adresse de la page : chaque paramètre prend une valeur d'exemple, un paramètre facultatif est omis. */
function samplePath(route: RouteObject): string {
  if (route.index) return '/'
  const path = (route.path ?? '').replace(/\/:[^/]+\?/g, '').replace(/:[^/]+/g, 'exemple')
  return `/${path}`
}

/**
 * Les pages publiques qu'un moteur n'a pas à explorer : la connexion, l'essai, le défi du jour (interactif, il change
 * chaque jour) et les profils publics.
 */
const UNLISTED_PUBLIC_PAGES = ['/auth/', '/try/', '/daily/', '/u/']

describe('robots.txt (ADR 0065)', () => {
  it('speaks to every crawler, in a single group', () => {
    expect(robots.match(/^User-agent:.*$/gm)).toEqual(['User-agent: *'])
  })

  it.each(accountPages.map(samplePath))(
    'keeps the crawlers away from %s, an account page',
    (path) => {
      expect(isBlocked(path)).toBe(true)
    },
  )

  it('reads the account pages, the level check among them (ADR 0068)', () => {
    expect(accountPages.map(samplePath)).toContain('/subjects/exemple/level-check')
  })

  const publicPages = pages.filter((route) => route.index || (route.path && route.path !== '*'))

  it.each(publicPages.map(samplePath))('decides what to do with %s, a public page', (path) => {
    const unlisted = UNLISTED_PUBLIC_PAGES.some((prefix) => path.startsWith(prefix))
    expect(isBlocked(path)).toBe(unlisted)
  })

  it('reads the public pages, with at least the home page and the Tour', () => {
    expect(publicPages.map(samplePath)).toEqual(
      expect.arrayContaining(['/', '/subjects/exemple/course/exemple', '/subjects/exemple/tour']),
    )
  })

  it('keeps the visit counter of the funnel out of reach, but leaves the rest of the API open', () => {
    expect(isBlocked('/api/v1/funnel/visits')).toBe(true)
    expect(isBlocked('/api/v1/subjects')).toBe(false)
    expect(isBlocked('/api/v1/subjects/go/tour/hello-world')).toBe(false)
    expect(rules().map((rule) => rule.path)).not.toContain('/api/')
  })

  it('leaves the sitemap and the share image open', () => {
    expect(isBlocked('/sitemap.xml')).toBe(false)
    expect(isBlocked('/og-image.png')).toBe(false)
  })

  it('points to the sitemap, at the public origin that nginx fills in', () => {
    expect(robots).toMatch(/^Sitemap: __PUBLIC_ORIGIN__\/sitemap\.xml$/m)
  })
})

describe('the share tags of index.html (ADR 0065)', () => {
  // Lue telle quelle sur le disque, puis analysée : les commentaires du fichier n'en font pas partie.
  const html = new DOMParser().parseFromString(readFileSync('index.html', 'utf8'), 'text/html')
  const meta = (attribute: 'property' | 'name', key: string) =>
    html.head.querySelector(`meta[${attribute}="${key}"]`)?.getAttribute('content')

  it('describes every page for a link preview, with an absolute image at the public origin', () => {
    expect(meta('property', 'og:type')).toBe('website')
    expect(meta('property', 'og:site_name')).toBe('Recallio')
    expect(meta('property', 'og:locale')).toBe('fr_FR')
    expect(meta('property', 'og:title')).toContain('Recallio')
    expect(meta('property', 'og:description')).toBeTruthy()
    expect(meta('property', 'og:image')).toBe('__PUBLIC_ORIGIN__/og-image.png')
    expect(meta('property', 'og:image:width')).toBe('1200')
    expect(meta('property', 'og:image:height')).toBe('630')
    expect(meta('property', 'og:image:alt')).toBeTruthy()
    expect(meta('name', 'twitter:card')).toBe('summary_large_image')
  })

  // Fixes, ils diraient que toutes les pages sont la même : un moteur ne garderait que l'accueil.
  it('declares neither a canonical address nor og:url', () => {
    expect(html.head.querySelector('link[rel="canonical"]')).toBeNull()
    expect(meta('property', 'og:url')).toBeUndefined()
  })
})
