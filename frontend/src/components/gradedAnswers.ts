import type { components } from '../api/schema'

/** Une question que le serveur corrige : un vrai ou faux, un QCM ou un code à compléter (ADR 0037, 0038). */
export type GradedQuestionData = {
  cardId: string
  type: components['schemas']['CardType']
  prompt: string
  choices: string[]
  multipleAnswers: boolean
  blankCount: number
}

/** Sa correction par le serveur. */
export type GradedCorrection = {
  correct: boolean
  correctChoices: number[]
  blanks: { expected: string; correct: boolean }[]
  explanation: string
}

/** Une réponse en cours : les choix cochés d'un QCM ou d'un vrai ou faux, ou les trous d'un code à compléter. */
export type AnswerDraft = { choices: number[]; blanks: string[] }

export function emptyDraft(question: GradedQuestionData): AnswerDraft {
  return { choices: [], blanks: Array<string>(question.blankCount).fill('') }
}

/** Vrai quand la réponse peut partir : au moins un choix coché, ou chaque trou rempli. */
export function isDraftComplete(question: GradedQuestionData, draft: AnswerDraft): boolean {
  return question.type === 'FILL_IN'
    ? draft.blanks.every((blank) => blank.trim() !== '')
    : draft.choices.length > 0
}

/** La réponse telle que le serveur l'attend. */
export function toGradedAnswer(question: GradedQuestionData, draft: AnswerDraft) {
  return question.type === 'FILL_IN'
    ? { cardId: question.cardId, filledBlanks: draft.blanks }
    : { cardId: question.cardId, selectedChoices: draft.choices }
}
