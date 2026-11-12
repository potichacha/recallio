import { describe, expect, it } from 'vitest'
import type { Progress } from '../api/progress'
import { ricoWord } from './ricoWord'

const progress = (overrides: Partial<Progress> = {}): Progress => ({
  totalXp: 120,
  streak: 3,
  streakFreezes: 0,
  daysToNextFreeze: 4,
  freezeEarnedToday: false,
  cardsToday: 4,
  dailyGoal: 10,
  goalReachedToday: false,
  subjects: [],
  ...overrides,
})

const said = (overrides: Partial<Parameters<typeof ricoWord>[0]> = {}) =>
  ricoWord({
    firstName: 'Alice',
    progress: progress(),
    newcomer: false,
    nothingToReview: false,
    ...overrides,
  })

describe('what Rico says on the home (ADR 0058)', () => {
  it('welcomes a newcomer without a word about a streak at zero', () => {
    expect(said({ newcomer: true, progress: progress({ streak: 0, cardsToday: 0 }) })).toEqual({
      greeting: 'Bienvenue Alice !',
      message: 'Ta première session dure dix minutes. On y va ?',
      mood: 'happy',
    })
  })

  it('tells how many cards are left to make the streak grow', () => {
    expect(said()).toEqual({
      greeting: 'Salut Alice !',
      message: 'Encore 6 cartes et ta série passe à 4 jours.',
      mood: 'cheering',
    })
    expect(said({ progress: progress({ cardsToday: 9 }) }).message).toBe(
      'Encore 1 carte et ta série passe à 4 jours.',
    )
  })

  it('invites to start a streak when there is none', () => {
    expect(said({ progress: progress({ streak: 0 }) }).message).toBe(
      'Encore 6 cartes pour lancer ta série.',
    )
  })

  it('celebrates the goal of the day once reached', () => {
    expect(
      said({ progress: progress({ cardsToday: 10, goalReachedToday: true, streak: 4 }) }),
    ).toEqual({
      greeting: 'Salut Alice !',
      message: 'Objectif atteint : 4 jours de suite. À demain !',
      mood: 'happy',
    })
  })

  it('says when the subject is up to date, rather than asking for cards that do not exist', () => {
    expect(said({ nothingToReview: true })).toMatchObject({
      message: 'Tout est à jour ici pour le moment. Ouvre un autre sujet, ou reviens plus tard.',
      mood: 'thinking',
    })
  })

  it('greets without a name when it does not know it', () => {
    expect(said({ firstName: undefined }).greeting).toBe('Salut !')
  })
})
