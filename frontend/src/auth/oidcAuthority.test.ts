/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import {
  LOCAL_OIDC_AUTHORITY,
  OIDC_AUTHORITY_MARKER,
  OIDC_AUTHORITY_TAG,
  oidcAuthority,
} from './oidcAuthority'

const PRODUCTION = 'https://auth.recallio.test/realms/recallio'
const BUILD = 'http://localhost:9180/realms/recallio'

describe('the address of the OIDC provider (ADR 0069)', () => {
  it('takes the one nginx put in index.html, over the one given at build time', () => {
    expect(oidcAuthority(PRODUCTION, BUILD)).toBe(PRODUCTION)
  })

  it('falls back on the one given at build time when the tag keeps its marker, as with npm run dev', () => {
    expect(oidcAuthority(OIDC_AUTHORITY_MARKER, BUILD)).toBe(BUILD)
  })

  it('falls back on the one given at build time when the tag is missing or empty', () => {
    expect(oidcAuthority(undefined, BUILD)).toBe(BUILD)
    expect(oidcAuthority('', BUILD)).toBe(BUILD)
  })

  it('falls back on the local Keycloak when nothing else is given', () => {
    expect(oidcAuthority(OIDC_AUTHORITY_MARKER, undefined)).toBe(LOCAL_OIDC_AUTHORITY)
    expect(oidcAuthority(undefined, undefined)).toBe(LOCAL_OIDC_AUTHORITY)
  })

  it('finds in index.html the tag that nginx fills in, with its marker', () => {
    const html = new DOMParser().parseFromString(readFileSync('index.html', 'utf8'), 'text/html')
    const tag = html.head.querySelector(`meta[name="${OIDC_AUTHORITY_TAG}"]`)

    expect(tag?.getAttribute('content')).toBe(OIDC_AUTHORITY_MARKER)
  })
})
