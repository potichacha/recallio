import { useEffect, useRef, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { useSubjects } from '../api/content'
import { ApiError } from '../api/errors'
import {
  useMockExam,
  useMockExamHistory,
  useStartMockExam,
  useSubmitMockExam,
  type MockExam,
  type MockExamResult,
} from '../api/mockExams'
import { GradedQuestion } from '../components/GradedQuestion'
import {
  emptyDraft,
  isDraftComplete,
  toGradedAnswer,
  type AnswerDraft,
} from '../components/gradedAnswers'
import { useCountdown } from '../components/useCountdown'
import { button } from '../components/ui/button'

const DATE = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium' })

/** Une copie notée, avec les réponses données, pour les montrer à côté du corrigé. */
type Graded = { result: MockExamResult; drafts: Record<string, AnswerDraft> }
const PRIMARY = `self-start ${button()}`

/**
 * L'examen blanc d'un sujet (ADR 0044) : des questions corrigées par le serveur, un temps limité, puis la note et le
 * corrigé. L'identifiant de l'examen en cours est dans l'adresse, pour le reprendre après un rechargement.
 */
export function MockExamPage() {
  const { slug = '' } = useParams()
  const [params, setParams] = useSearchParams()
  const examId = params.get('exam')
  const subjects = useSubjects()
  const title = subjects.data?.find((subject) => subject.slug === slug)?.title ?? slug
  const start = useStartMockExam()
  const resumed = useMockExam(examId)
  const [graded, setGraded] = useState<Graded>()

  const begin = () =>
    start.mutate(slug, {
      onSuccess: (exam) => {
        setGraded(undefined)
        setParams({ exam: exam.id }, { replace: true })
      },
    })

  const exam = start.data?.id === examId ? start.data : resumed.data
  return (
    <section className="flex flex-col gap-5">
      <h1 className="text-2xl font-bold">Examen blanc : {title}</h1>
      {graded ? (
        <Result graded={graded} exam={exam} onRestart={begin} restarting={start.isPending} />
      ) : exam && !exam.submitted ? (
        <Running key={exam.id} exam={exam} subject={slug} onSubmitted={setGraded} />
      ) : (
        <Intro
          subject={slug}
          onStart={begin}
          starting={start.isPending}
          error={start.error}
          alreadySubmitted={exam?.submitted ?? false}
        />
      )}
      <Link to="/" className="self-start text-sm text-brand hover:underline">
        Retour au tableau de bord
      </Link>
    </section>
  )
}

function Intro({
  subject,
  onStart,
  starting,
  error,
  alreadySubmitted,
}: {
  subject: string
  onStart: () => void
  starting: boolean
  error: Error | null
  alreadySubmitted: boolean
}) {
  const history = useMockExamHistory(subject)
  return (
    <div className="flex flex-col gap-4">
      <p className="text-ink-soft">
        Jusqu’à 20 questions corrigées par le serveur, une minute par question, les notions
        essentielles d’abord, comme en entretien. À la fin, la note et le corrigé. Rien n’entre dans
        tes révisions.
      </p>
      {alreadySubmitted && <p role="status">Cette copie est déjà rendue.</p>}
      <button type="button" onClick={onStart} disabled={starting} className={PRIMARY}>
        Commencer l’examen
      </button>
      {error && (
        <p role="alert">
          {error instanceof ApiError ? error.message : 'L’examen n’a pas pu commencer.'}
        </p>
      )}
      {history.data && history.data.length > 0 && (
        <section aria-labelledby="exam-history-title" className="flex flex-col gap-2">
          <h2 id="exam-history-title" className="text-lg font-semibold">
            Mes dernières notes
          </h2>
          <ul className="flex flex-col gap-1 text-sm">
            {history.data.map((past) => (
              <li key={past.id}>
                {DATE.format(new Date(past.startedAt))} : {past.correct} / {past.total} (
                {past.percent} %){past.inTime ? '' : ', hors délai'}
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}

function Running({
  exam,
  subject,
  onSubmitted,
}: {
  exam: MockExam
  subject: string
  onSubmitted: (graded: Graded) => void
}) {
  const [drafts, setDrafts] = useState<Record<string, AnswerDraft>>(() =>
    Object.fromEntries(exam.questions.map((question) => [question.cardId, emptyDraft(question)])),
  )
  const submit = useSubmitMockExam(subject)
  const remaining = useCountdown(exam.remainingMs)
  const sentByTimer = useRef(false)

  const hand = () => {
    const answers = exam.questions
      .filter((question) => isDraftComplete(question, drafts[question.cardId]))
      .map((question) => toGradedAnswer(question, drafts[question.cardId]))
    submit.mutate(
      { id: exam.id, answers },
      { onSuccess: (result) => onSubmitted({ result, drafts }) },
    )
  }
  // Le temps écoulé, la copie part d'elle-même, telle quelle, une seule fois : en cas d'échec, le bouton reste.
  useEffect(() => {
    if (remaining === 0 && !sentByTimer.current) {
      sentByTimer.current = true
      hand()
    }
  })

  const answered = exam.questions.filter((question) =>
    isDraftComplete(question, drafts[question.cardId]),
  ).length
  return (
    <div className="flex flex-col gap-5">
      <p
        role="timer"
        aria-label="Temps restant"
        className="sticky top-0 self-start rounded-lg bg-surface px-3 py-1 font-mono text-lg shadow-sm"
      >
        {clock(remaining)}
      </p>
      <ol className="flex flex-col gap-4">
        {exam.questions.map((question, index) => (
          <li key={question.cardId} className="rounded-xl bg-surface p-4 shadow-sm">
            <GradedQuestion
              question={question}
              label={`Question ${index + 1} sur ${exam.questions.length}`}
              draft={drafts[question.cardId]}
              correction={undefined}
              onChange={(draft) => setDrafts((all) => ({ ...all, [question.cardId]: draft }))}
            />
          </li>
        ))}
      </ol>
      <p className="text-sm text-ink-muted">
        {answered} / {exam.questions.length} questions répondues
      </p>
      <button type="button" onClick={hand} disabled={submit.isPending} className={PRIMARY}>
        Rendre ma copie
      </button>
      {submit.isError && (
        <p role="alert">
          {submit.error instanceof ApiError
            ? submit.error.message
            : 'La copie n’a pas pu partir : réessaie.'}
        </p>
      )}
    </div>
  )
}

function Result({
  graded: { result, drafts },
  exam,
  onRestart,
  restarting,
}: {
  graded: Graded
  exam: MockExam | undefined
  onRestart: () => void
  restarting: boolean
}) {
  const questions = new Map(exam?.questions.map((question) => [question.cardId, question]))
  return (
    <div className="flex flex-col gap-5">
      <div role="status" className="flex flex-col gap-1 rounded-xl bg-brand p-5 text-on-brand">
        <p className="text-2xl font-bold">
          {result.correct} / {result.total}
        </p>
        <p>
          {result.percent} % de bonnes réponses
          {result.inTime ? '' : ' : copie rendue hors délai'}
        </p>
      </div>
      <ol className="flex flex-col gap-4">
        {result.results.map((correction, index) => {
          const question = questions.get(correction.cardId)
          if (!question) return null
          return (
            <li key={correction.cardId} className="rounded-xl bg-surface p-4 shadow-sm">
              <GradedQuestion
                question={question}
                label={`Question ${index + 1} : ${correction.correct ? 'juste' : 'fausse'}`}
                draft={drafts[correction.cardId] ?? emptyDraft(question)}
                correction={correction}
                onChange={() => {}}
              />
            </li>
          )
        })}
      </ol>
      <button type="button" onClick={onRestart} disabled={restarting} className={PRIMARY}>
        Repasser un examen
      </button>
    </div>
  )
}

/** 754 000 ms → « 12:34 ». */
function clock(ms: number) {
  const seconds = Math.ceil(ms / 1000)
  return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`
}
