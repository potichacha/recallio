import { describe, expect, it } from 'vitest'
import { FAILED_DOMAINS_TO_STOP, isOver, notionCount } from './levelCheckRun'

const passed = (domain: string) => ({ domain, passed: true })
const failed = (domain: string) => ({ domain, passed: false })

describe('the run of a level check (ADR 0068)', () => {
  it('goes on until every domain is asked', () => {
    expect(isOver([passed('a'), failed('b')], 3)).toBe(false)
    expect(isOver([passed('a'), failed('b'), passed('c')], 3)).toBe(true)
  })

  it('stops after two failed domains in a row, and only in a row', () => {
    expect(FAILED_DOMAINS_TO_STOP).toBe(2)
    expect(isOver([failed('a')], 12)).toBe(false)
    expect(isOver([failed('a'), passed('b'), failed('c')], 12)).toBe(false)
    expect(isOver([passed('a'), failed('b'), failed('c')], 12)).toBe(true)
  })

  it('counts the notions in French', () => {
    expect(notionCount(1)).toBe('1 notion')
    expect(notionCount(9)).toBe('9 notions')
  })
})
