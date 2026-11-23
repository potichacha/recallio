import type { Breadcrumb, ErrorEvent as SentryEvent } from '@sentry/browser'

/**
 * Ce qui ne part jamais chez Sentry (ADR 0070) : une adresse y arrive sans chaîne de requête ni fragment (le code et
 * l'état du retour OIDC, une recherche), et sans le jeton d'un lien d'invitation ni l'adresse d'un profil public. Les
 * UUID des ressources (parties, duels, défis) restent : ils ne disent rien de personne.
 */
const SENSITIVE_SEGMENTS: ReadonlyArray<readonly [RegExp, string]> = [
  // Le jeton d'un lien d'invitation, dans la page et dans l'API.
  [/(\/join\/)[^/?#\s"']+/g, '$1:token'],
  [/(\/invitations\/)[^/?#\s"']+/g, '$1:token'],
  // L'adresse d'un profil public, dans la page et dans l'API.
  [/(\/u\/)[^/?#\s"']+/g, '$1:handle'],
  [/(\/public-profiles\/)[^/?#\s"']+/g, '$1:handle'],
]

/** Les clés d'un fil d'Ariane qui portent une adresse : celle d'un appel (`fetch`, `xhr`), celles d'une navigation. */
const URL_KEYS = new Set(['url', 'from', 'to'])

function maskSegments(text: string): string {
  return SENSITIVE_SEGMENTS.reduce(
    (masked, [pattern, replacement]) => masked.replace(pattern, replacement),
    text,
  )
}

/** Une adresse sans chaîne de requête ni fragment, son jeton d'invitation et son adresse de profil masqués. */
export function scrubUrl(url: string): string {
  return maskSegments(url.replace(/[?#].*$/s, ''))
}

/**
 * Un texte, un message d'erreur par exemple, dont chaque adresse est nettoyée comme par {@link scrubUrl}. Une chaîne de
 * requête suit un caractère de l'adresse, jamais une espace : le « ? » d'une phrase reste en place.
 */
export function scrubText(text: string): string {
  return maskSegments(text.replace(/([^\s?#])[?#][^\s"'<>]+/g, '$1'))
}

/** Un fil d'Ariane nettoyé ; `null` pour ceux de la console, qui ne partent jamais. */
export function scrubBreadcrumb(breadcrumb: Breadcrumb): Breadcrumb | null {
  if (breadcrumb.category === 'console') return null
  return {
    ...breadcrumb,
    message: breadcrumb.message && scrubText(breadcrumb.message),
    data:
      breadcrumb.data &&
      Object.fromEntries(
        Object.entries(breadcrumb.data).map(([key, value]) => [
          key,
          URL_KEYS.has(key) && typeof value === 'string' ? scrubUrl(value) : value,
        ]),
      ),
  }
}

/**
 * Une erreur nettoyée avant l'envoi : l'adresse de la page et le Referer, les messages, les fils d'Ariane. Ni
 * utilisateur, ni cookies, ni chaîne de requête, ni corps de requête.
 */
export function scrubEvent(event: SentryEvent): SentryEvent {
  const scrubbed: SentryEvent = {
    ...event,
    message: event.message && scrubText(event.message),
    logentry: event.logentry && {
      ...event.logentry,
      message: event.logentry.message && scrubText(event.logentry.message),
    },
    transaction: event.transaction && scrubUrl(event.transaction),
    request: event.request && scrubRequest(event.request),
    exception: event.exception && {
      ...event.exception,
      values: event.exception.values?.map((exception) => ({
        ...exception,
        value: exception.value && scrubText(exception.value),
      })),
    },
    breadcrumbs: event.breadcrumbs
      ?.map(scrubBreadcrumb)
      .filter((breadcrumb): breadcrumb is Breadcrumb => breadcrumb !== null),
  }
  delete scrubbed.user
  return scrubbed
}

type SentryRequest = NonNullable<SentryEvent['request']>

function scrubRequest(request: SentryRequest): SentryRequest {
  const scrubbed: SentryRequest = {
    ...request,
    url: request.url && scrubUrl(request.url),
    headers:
      request.headers &&
      Object.fromEntries(
        Object.entries(request.headers).map(([name, value]) => [
          name,
          name.toLowerCase() === 'referer' ? scrubUrl(value) : value,
        ]),
      ),
  }
  delete scrubbed.query_string
  delete scrubbed.cookies
  delete scrubbed.data
  return scrubbed
}
