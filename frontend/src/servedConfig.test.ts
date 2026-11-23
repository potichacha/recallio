import { afterEach, describe, expect, it } from 'vitest'
import { metaContent, servedValue } from './servedConfig'

const MARKER = '__RECALLIO_VALUE__'

describe('the configuration that nginx puts in index.html (ADR 0069)', () => {
  afterEach(() => document.head.querySelector('meta[name="recallio-value"]')?.remove())

  it('reads the content of a meta tag', () => {
    const tag = document.createElement('meta')
    tag.name = 'recallio-value'
    tag.content = 'https://recallio.test'
    document.head.append(tag)

    expect(metaContent('recallio-value')).toBe('https://recallio.test')
  })

  it('finds nothing when the tag is missing', () => {
    expect(metaContent('recallio-value')).toBeUndefined()
  })

  it('takes the value that nginx put in place of the marker', () => {
    expect(servedValue('https://recallio.test', MARKER)).toBe('https://recallio.test')
  })

  it('ignores a tag that keeps its marker, as with npm run dev, an empty one or a missing one', () => {
    expect(servedValue(MARKER, MARKER)).toBeUndefined()
    expect(servedValue('', MARKER)).toBeUndefined()
    expect(servedValue(undefined, MARKER)).toBeUndefined()
  })
})
