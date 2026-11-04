import { describe, expect, it } from 'vitest'
import type { NextMasteryLevel } from '../api/learningPath'
import { masterySummary, nextLevelHint } from './mastery'

const next = (overrides: Partial<NextMasteryLevel>): NextMasteryLevel => ({
  level: 'ACQUIRED',
  cards: 0,
  cardsNeeded: 5,
  formats: 0,
  formatsNeeded: 0,
  missingFormats: [],
  ...overrides,
})

describe('nextLevelHint', () => {
  it('says what leads to each next level, with the thresholds given by the server', () => {
    expect(nextLevelHint(next({ level: 'DISCOVERED', cardsNeeded: 1 }))).toBe(
      'Pour la découvrir : voir une première carte',
    )
    expect(nextLevelHint(next({ level: 'ACQUIRED', cards: 3 }))).toBe(
      'Pour l’acquérir : 3 / 5 cartes réussies',
    )
    expect(nextLevelHint(next({ level: 'MASTERED', cards: 4, formats: 2, formatsNeeded: 3 }))).toBe(
      'Pour la maîtriser : 4 / 5 cartes maîtrisées, 2 / 3 formats',
    )
  })

  it('names the formats still to succeed to become an expert', () => {
    expect(
      nextLevelHint(
        next({
          level: 'EXPERT',
          cards: 1,
          formats: 1,
          formatsNeeded: 3,
          missingFormats: ['MULTIPLE_CHOICE', 'BUG_FIX', 'CODING_EXERCISE'],
        }),
      ),
    ).toBe(
      'Pour devenir experte : 1 / 5 cartes retenues sur la durée, 1 / 3 formats ; à réussir : un QCM, un bug à corriger et l’exercice de code',
    )
  })

  it('agrees with a small notion of a single card', () => {
    expect(nextLevelHint(next({ level: 'MASTERED', cardsNeeded: 1, formatsNeeded: 1 }))).toBe(
      'Pour la maîtriser : 0 / 1 carte maîtrisée, 0 / 1 format',
    )
  })
})

describe('masterySummary', () => {
  it('counts the notions acquired, mastered and expert', () => {
    expect(masterySummary({ total: 12, discovered: 5, acquired: 3, mastered: 2, expert: 0 })).toBe(
      '3 / 12 notions acquises · 2 maîtrisées · 0 experte',
    )
  })
})
