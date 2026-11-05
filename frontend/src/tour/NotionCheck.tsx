import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/errors'
import {
  useCheckTourNotion,
  useTourCheckpoint,
  type TourAnswer,
  type TourCheck,
  type TourQuestion,
} from '../api/tour'
import { ChoiceOption } from '../components/ChoiceOption'
import { choiceOutcome, toggleChoice } from '../components/choices'
import { Markdown } from '../components/Markdown'
import { numberBlanks } from '../review/blanks'

type QuestionResult = TourCheck['results'][number]

/** Une réponse en cours : les choix cochés d'un QCM ou d'un vrai ou faux, ou les trous d'un code à compléter. */
type Answer = { choices: number[]; blanks: string[] }

/**
 * Les questions qui valident une notion du Tour (ADR 0037) : on y répond, le serveur corrige, et la notion est validée
 * quand toutes sont justes. On peut réessayer autant de fois qu'on veut ; ces réponses ne comptent pas comme des
 * révisions.
 */
export function NotionCheck({ subject, notion }: { subject: string; notion: string }) {
  const checkpoint = useTourCheckpoint(subject, notion)
  const check = useCheckTourNotion(subject, notion)
  const [answers, setAnswers] = useState<Record<string, Answer>>({})

  if (checkpoint.isError) {
    return <p role="alert">Impossible de charger les questions de cette notion.</p>
  }
  if (!checkpoint.data) return null

  const { questions } = checkpoint.data
  const answerOf = (question: TourQuestion): Answer =>
    answers[question.cardId] ?? { choices: [], blanks: Array(question.blankCount).fill('') }
  const update = (question: TourQuestion, answer: Answer) =>
    setAnswers((current) => ({ ...current, [question.cardId]: answer }))
  const results = new Map(check.data?.results.map((result) => [result.cardId, result]))
  const validated = check.data?.validated ?? checkpoint.data.validated
  const complete = questions.every((question) =>
    question.type === 'FILL_IN'
      ? answerOf(question).blanks.every((blank) => blank.trim() !== '')
      : answerOf(question).choices.length > 0,
  )

  const submit = (event: FormEvent) => {
    event.preventDefault()
    check.mutate(questions.map((question) => toAnswer(question, answerOf(question))))
  }

  return (
    <section
      aria-labelledby={`check-${notion}`}
      className="flex flex-col gap-4 rounded-xl bg-white p-5 shadow-sm"
    >
      <h2 id={`check-${notion}`} className="text-lg font-semibold">
        Valider la notion
      </h2>
      {validated ? (
        <p role="status" className="font-medium text-emerald-700">
          Notion validée !
        </p>
      ) : (
        <p className="text-sm text-slate-600">
          Réponds juste à ces questions pour valider la notion. Elles ne comptent pas dans tes
          révisions.
        </p>
      )}
      <form onSubmit={submit} className="flex flex-col gap-5">
        {questions.map((question, index) => (
          <QuestionField
            key={question.cardId}
            question={question}
            number={index + 1}
            answer={answerOf(question)}
            result={results.get(question.cardId)}
            onChange={(answer) => update(question, answer)}
          />
        ))}
        {check.data ? (
          !check.data.validated && (
            <button
              type="button"
              onClick={() => check.reset()}
              className="self-start rounded-lg border border-indigo-200 px-4 py-2 font-medium text-indigo-700 hover:bg-indigo-50"
            >
              Réessayer
            </button>
          )
        ) : (
          <button
            type="submit"
            disabled={!complete || check.isPending}
            className="self-start rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            {check.isPending ? 'Vérification…' : 'Vérifier'}
          </button>
        )}
      </form>
      {check.error && (
        <p role="alert" className="text-sm text-rose-700">
          {check.error instanceof ApiError ? check.error.message : 'La vérification a échoué.'}
        </p>
      )}
    </section>
  )
}

function QuestionField({
  question,
  number,
  answer,
  result,
  onChange,
}: {
  question: TourQuestion
  number: number
  answer: Answer
  result: QuestionResult | undefined
  onChange: (answer: Answer) => void
}) {
  const label = `Question ${number}`
  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="text-sm font-medium text-slate-500">{label}</legend>
      {question.type === 'FILL_IN' ? (
        <>
          <Markdown>{numberBlanks(question.prompt)}</Markdown>
          {answer.blanks.map((value, index) => (
            <label key={index} className="flex flex-col gap-1">
              <span className="text-sm font-medium">Trou {index + 1}</span>
              <input
                value={value}
                onChange={(event) =>
                  onChange({
                    ...answer,
                    blanks: answer.blanks.map((other, at) =>
                      at === index ? event.target.value : other,
                    ),
                  })
                }
                readOnly={result !== undefined}
                spellCheck={false}
                autoCapitalize="off"
                autoComplete="off"
                className="rounded-lg border border-slate-300 px-3 py-2 font-mono text-sm"
              />
              {result?.blanks[index] && (
                <span
                  className={`text-sm ${result.blanks[index].correct ? 'text-emerald-700' : 'text-rose-700'}`}
                >
                  {result.blanks[index].correct
                    ? 'Juste'
                    : `Attendu : ${result.blanks[index].expected}`}
                </span>
              )}
            </label>
          ))}
        </>
      ) : (
        <>
          <Markdown>{question.prompt}</Markdown>
          {question.choices.map((choice, index) => (
            <ChoiceOption
              key={index}
              name={question.cardId}
              text={choice}
              type={question.multipleAnswers ? 'checkbox' : 'radio'}
              checked={answer.choices.includes(index)}
              onChange={() =>
                onChange({
                  ...answer,
                  choices: toggleChoice(answer.choices, index, question.multipleAnswers),
                })
              }
              outcome={
                result
                  ? choiceOutcome(result.correctChoices, index, answer.choices.includes(index))
                  : undefined
              }
            />
          ))}
        </>
      )}
      {result && (
        <div
          className={`rounded-lg p-3 text-sm ${result.correct ? 'bg-emerald-50' : 'bg-rose-50'}`}
        >
          <p className="font-medium">{result.correct ? 'Juste !' : 'Pas tout à fait.'}</p>
          <Markdown>{result.explanation}</Markdown>
        </div>
      )}
    </fieldset>
  )
}

function toAnswer(question: TourQuestion, answer: Answer): TourAnswer {
  return question.type === 'FILL_IN'
    ? { cardId: question.cardId, filledBlanks: answer.blanks }
    : { cardId: question.cardId, selectedChoices: answer.choices }
}
