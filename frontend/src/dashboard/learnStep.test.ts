import { describe, expect, it } from 'vitest'
import type { ThemeStats } from '../api/stats'
import type { TourOutline } from '../api/tour'
import { courseLearnStep, tourLearnStep } from './learnStep'

const entry = (slug: string, title: string, pageCount = 1) => ({ slug, title, pageCount })

const OUTLINE: TourOutline = {
  subjectSlug: 'go',
  subjectTitle: 'Go',
  domains: [
    {
      id: 'bases',
      title: 'Les bases',
      chapters: [
        {
          id: 'premiers-pas',
          title: 'Premiers pas',
          notions: [entry('bienvenue', 'Bienvenue'), entry('paquets', 'Les paquets', 2)],
          exercises: [],
        },
        {
          id: 'types',
          title: 'Les types',
          notions: [entry('entiers', 'Les entiers'), entry('chaines', 'Les chaînes')],
          exercises: [],
        },
      ],
    },
  ],
}

const theme = (title: string, seen: number, total: number): ThemeStats => ({
  slug: title.toLowerCase(),
  title,
  counts: { total, seen, mastered: 0, dueNow: 0, dueToday: 0 },
})

describe('the step of the « Apprendre » card, for a subject with a Tour (ADR 0077)', () => {
  it('starts at the first lesson of a Tour never opened', () => {
    expect(tourLearnStep('go', OUTLINE, { validatedNotions: [], lastPage: null })).toEqual({
      to: '/subjects/go/tour/bienvenue',
      chapter: 'Premiers pas',
      done: 0,
      total: 2,
      unit: 'lesson',
      next: 'Bienvenue',
      started: false,
    })
  })

  it('resumes at the last page opened, in its chapter', () => {
    const step = tourLearnStep('go', OUTLINE, {
      validatedNotions: ['bienvenue', 'paquets', 'entiers'],
      lastPage: { notion: 'chaines', page: 1 },
    })

    expect(step).toMatchObject({
      to: '/subjects/go/tour/chaines',
      chapter: 'Les types',
      done: 1,
      total: 2,
      next: 'Les chaînes',
      started: true,
    })
  })

  it('goes to the first lesson to discover when the last page left the Tour', () => {
    const step = tourLearnStep('go', OUTLINE, {
      validatedNotions: ['bienvenue'],
      lastPage: { notion: 'retiree', page: 3 },
    })

    expect(step).toMatchObject({ to: '/subjects/go/tour/paquets', chapter: 'Premiers pas' })
  })

  it('skips the lessons known at the level check, and counts them as done (ADR 0068)', () => {
    const step = tourLearnStep('go', OUTLINE, {
      validatedNotions: [],
      knownNotions: ['bienvenue', 'paquets'],
      lastPage: null,
    })

    expect(step).toMatchObject({ to: '/subjects/go/tour/entiers', chapter: 'Les types', done: 0 })
    // Rien n'a encore été ouvert ni validé : on commence.
    expect(step?.started).toBe(false)
  })

  it('goes back to the first page once every lesson is done', () => {
    const step = tourLearnStep('go', OUTLINE, {
      validatedNotions: ['bienvenue', 'paquets', 'entiers', 'chaines'],
      lastPage: null,
    })

    expect(step).toMatchObject({ to: '/subjects/go/tour/bienvenue', done: 2, total: 2 })
  })

  it('has no step in an empty Tour', () => {
    const empty = { ...OUTLINE, domains: [] }

    expect(tourLearnStep('go', empty, { validatedNotions: [], lastPage: null })).toBeUndefined()
  })
})

describe('the step of the « Apprendre » card, for a subject without a Tour (ADR 0077)', () => {
  it('stands in the first chapter whose cards were not all seen, and counts the chapters done', () => {
    const themes = [theme('Bases', 10, 10), theme('Objet', 3, 8), theme('Flux', 0, 6)]

    expect(courseLearnStep('spring-boot', themes)).toEqual({
      to: '/subjects/spring-boot/course',
      chapter: 'Objet',
      done: 1,
      total: 3,
      unit: 'chapter',
      started: true,
    })
  })

  it('has not started while no card was seen', () => {
    expect(courseLearnStep('sql', [theme('Bases', 0, 10)])).toMatchObject({
      chapter: 'Bases',
      done: 0,
      started: false,
    })
  })

  it('stays in the last chapter once everything was seen', () => {
    const themes = [theme('Bases', 10, 10), theme('Objet', 8, 8)]

    expect(courseLearnStep('sql', themes)).toMatchObject({ chapter: 'Objet', done: 2, total: 2 })
  })

  it('ignores a chapter without a card, and has no step in a subject without any', () => {
    expect(courseLearnStep('sql', [theme('Vide', 0, 0), theme('Bases', 0, 4)])).toMatchObject({
      chapter: 'Bases',
      total: 1,
    })
    expect(courseLearnStep('sql', [theme('Vide', 0, 0)])).toBeUndefined()
  })
})
