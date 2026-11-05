import type { CardType } from '../api/content'
import type { MasteryCounts, MasteryLevel, NextMasteryLevel } from '../api/learningPath'

/** Le niveau de maîtrise d'une notion (ADR 0034), accordé à « notion ». */
export const MASTERY_LABELS: Record<MasteryLevel, string> = {
  NEW: 'Nouvelle',
  DISCOVERED: 'Découverte',
  ACQUIRED: 'Acquise',
  MASTERED: 'Maîtrisée',
  EXPERT: 'Experte',
}

export const MASTERY_STYLES: Record<MasteryLevel, string> = {
  NEW: 'bg-surface-muted text-ink-muted',
  DISCOVERED: 'bg-warning-soft text-warning',
  ACQUIRED: 'bg-info-soft text-info',
  MASTERED: 'bg-success-soft text-success',
  EXPERT: 'bg-brand-soft text-brand',
}

/** Un format, tel qu'on le nomme dans une phrase : « il reste à réussir un QCM et l'exercice de code ». */
const FORMAT_NAMES: Record<CardType, string> = {
  FLASHCARD: 'une question de cours',
  CODE_OUTPUT: 'un « qu’affiche ce code ? »',
  OPEN_SCENARIO: 'une mise en situation',
  DEFINITION: 'une définition',
  MULTIPLE_CHOICE: 'un QCM',
  TRUE_FALSE: 'un vrai ou faux',
  BUG_FIX: 'un bug à corriger',
  FILL_IN: 'un code à compléter',
  CODING_EXERCISE: 'l’exercice de code',
}

const plural = (count: number, word: string) => `${word}${count > 1 ? 's' : ''}`

/** « a », « a et b », « a, b et c ». */
function enumerate(items: string[]): string {
  return items.length < 2 ? items.join('') : `${items.slice(0, -1).join(', ')} et ${items.at(-1)}`
}

/** Ce qui mène au niveau suivant, en une phrase : les seuils viennent du serveur (ADR 0034). */
export function nextLevelHint(next: NextMasteryLevel): string {
  const { cards, cardsNeeded, formats, formatsNeeded, missingFormats } = next
  const formatProgress = `${formats} / ${formatsNeeded} ${plural(formatsNeeded, 'format')}`
  switch (next.level) {
    case 'NEW':
    case 'DISCOVERED':
      return 'Pour la découvrir : voir une première carte'
    case 'ACQUIRED':
      return `Pour l’acquérir : ${cards} / ${cardsNeeded} ${plural(cardsNeeded, 'carte')} ${plural(cardsNeeded, 'réussie')}`
    case 'MASTERED':
      return `Pour la maîtriser : ${cards} / ${cardsNeeded} ${plural(cardsNeeded, 'carte')} ${plural(cardsNeeded, 'maîtrisée')}, ${formatProgress}`
    case 'EXPERT': {
      const toSucceed = missingFormats.length
        ? ` ; à réussir : ${enumerate(missingFormats.map((format) => FORMAT_NAMES[format]))}`
        : ''
      return `Pour devenir experte : ${cards} / ${cardsNeeded} ${plural(cardsNeeded, 'carte')} ${plural(cardsNeeded, 'retenue')} sur la durée, ${formatProgress}${toSucceed}`
    }
  }
}

/** « 3 / 12 notions acquises · 1 maîtrisée · 0 experte ». */
export function masterySummary(counts: MasteryCounts): string {
  const { total, acquired, mastered, expert } = counts
  return (
    `${acquired} / ${total} ${plural(total, 'notion')} ${plural(total, 'acquise')}` +
    ` · ${mastered} ${plural(mastered, 'maîtrisée')} · ${expert} ${plural(expert, 'experte')}`
  )
}
