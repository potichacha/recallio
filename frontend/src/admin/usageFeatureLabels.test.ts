/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { USAGE_FEATURE_LABELS } from './usageFeatureLabels'

// La spec fait foi (ADR 0003) : lue telle quelle sur le disque, là où `npm run generate:api` la lit aussi.
const spec = readFileSync('../backend/src/main/openapi/openapi.yaml', 'utf8')

/** Les valeurs de l'enum `UsageFeature` de la spec, dans leur ordre. */
function usageFeatures(): string[] {
  const block = /^ {4}UsageFeature:\n {6}type: string\n {6}enum:\n((?: {6}- \w+\n)+)/m.exec(spec)
  return [...(block?.[1] ?? '').matchAll(/- (\w+)/g)].map(([, value]) => value)
}

describe('the labels of the measured features (ADR 0076)', () => {
  it('reads the features from the OpenAPI spec', () => {
    expect(usageFeatures()).toContain('REVIEW')
    expect(usageFeatures().length).toBeGreaterThanOrEqual(14)
  })

  // Une fonctionnalité ajoutée au backend sans libellé s'afficherait « undefined » sur la page Bêta.
  it('names every feature of the API, and no other', () => {
    expect(Object.keys(USAGE_FEATURE_LABELS).sort()).toEqual(usageFeatures().sort())
    for (const label of Object.values(USAGE_FEATURE_LABELS)) expect(label.trim()).not.toBe('')
  })
})
