import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { MockExam, MockExamResult } from '../api/mockExams'
import {
  fakeSession,
  renderAt,
  sentRequests,
  SIGNED_IN_API,
  stubApi,
  TEST_SUBJECTS,
} from '../test/render'

const signedIn = () => fakeSession({ status: 'authenticated', userName: 'Alice' })

function exam(overrides: Partial<MockExam> = {}): MockExam {
  return {
    id: 'e1',
    subject: 'go',
    startedAt: '2026-11-05T10:00:00Z',
    remainingMs: 120_000,
    submitted: false,
    questions: [
      {
        cardId: 'c1',
        type: 'TRUE_FALSE',
        prompt: 'Une slice est un tableau.',
        choices: ['Vrai', 'Faux'],
        multipleAnswers: false,
        blankCount: 0,
      },
      {
        cardId: 'c2',
        type: 'FILL_IN',
        prompt: '`___ main`',
        choices: [],
        multipleAnswers: false,
        blankCount: 1,
      },
    ],
    ...overrides,
  }
}

const RESULT: MockExamResult = {
  correct: 1,
  total: 2,
  percent: 50,
  inTime: true,
  results: [
    { cardId: 'c1', correct: true, correctChoices: [1], blanks: [], explanation: 'Une vue.' },
    {
      cardId: 'c2',
      correct: false,
      correctChoices: [],
      blanks: [{ expected: 'package', correct: false }],
      explanation: 'Le paquet main.',
    },
  ],
}

function stubExam(started: MockExam = exam()) {
  return stubApi({
    ...SIGNED_IN_API,
    '/api/v1/subjects': { body: TEST_SUBJECTS },
    '/api/v1/me/mock-exams': {
      body: [
        {
          id: 'old',
          startedAt: '2026-11-01T09:00:00Z',
          correct: 14,
          total: 20,
          percent: 70,
          inTime: false,
        },
      ],
    },
    'POST /api/v1/me/mock-exams': { status: 201, body: started },
    '/api/v1/me/mock-exams/e1': { body: started },
    'POST /api/v1/me/mock-exams/e1/submission': { body: RESULT },
  })
}

describe('MockExamPage', () => {
  it('starts an exam, hands the copy, then shows the mark and the correction', async () => {
    const fetchMock = stubExam()
    renderAt('/subjects/go/exam', signedIn())

    expect(await screen.findByRole('heading', { name: 'Examen blanc : Go' })).toBeInTheDocument()
    expect(await screen.findByText('1 nov. 2026 : 14 / 20 (70 %), hors délai')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Commencer l’examen' }))

    expect(await screen.findByRole('timer', { name: 'Temps restant' })).toHaveTextContent('02:00')
    const first = screen.getByRole('group', { name: 'Question 1 sur 2' })
    await userEvent.click(within(first).getByRole('radio', { name: 'Faux' }))
    expect(screen.getByText('1 / 2 questions répondues')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Rendre ma copie' }))

    expect(await screen.findByRole('status')).toHaveTextContent('1 / 2')
    expect(screen.getByRole('status')).toHaveTextContent('50 % de bonnes réponses')
    expect(screen.getByRole('group', { name: 'Question 2 : fausse' })).toHaveTextContent(
      'Attendu : package',
    )
    const submission = sentRequests(fetchMock).find((request) =>
      request.url.endsWith('/submission'),
    )
    expect(await submission?.json()).toEqual({ answers: [{ cardId: 'c1', selectedChoices: [1] }] })
  })

  it('hands the copy by itself once the time is up', async () => {
    const fetchMock = stubExam(exam({ remainingMs: 0 }))
    renderAt('/subjects/go/exam?exam=e1', signedIn())

    expect(await screen.findByRole('status')).toHaveTextContent('1 / 2')
    const submission = sentRequests(fetchMock).find((request) =>
      request.url.endsWith('/submission'),
    )
    expect(await submission?.json()).toEqual({ answers: [] })
  })
})
