import { numberBlanks } from '../review/blanks'
import { ChoiceOption } from './ChoiceOption'
import { choiceOutcome, toggleChoice } from './choices'
import type { AnswerDraft, GradedCorrection, GradedQuestionData } from './gradedAnswers'
import { Markdown } from './Markdown'
import { useShuffledChoices } from './useShuffledChoices'

/** Une question à répondre, puis sa correction : le verdict de chaque choix ou de chaque trou, et l'explication. */
export function GradedQuestion({
  question,
  label,
  draft,
  correction,
  onChange,
}: {
  question: GradedQuestionData
  label: string
  draft: AnswerDraft
  correction: GradedCorrection | undefined
  onChange: (draft: AnswerDraft) => void
}) {
  const choices = useShuffledChoices(question.cardId, question.choices)
  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="text-sm font-medium text-ink-muted">{label}</legend>
      {question.type === 'FILL_IN' ? (
        <>
          <Markdown>{numberBlanks(question.prompt)}</Markdown>
          {draft.blanks.map((value, index) => (
            <label key={index} className="flex flex-col gap-1">
              <span className="text-sm font-medium">Trou {index + 1}</span>
              <input
                value={value}
                onChange={(event) =>
                  onChange({
                    ...draft,
                    blanks: draft.blanks.map((other, at) =>
                      at === index ? event.target.value : other,
                    ),
                  })
                }
                readOnly={correction !== undefined}
                spellCheck={false}
                autoCapitalize="off"
                autoComplete="off"
                className="rounded-lg border border-line-strong px-3 py-2 font-mono text-sm"
              />
              {correction?.blanks[index] && (
                <span
                  className={`text-sm ${correction.blanks[index].correct ? 'text-success' : 'text-danger'}`}
                >
                  {correction.blanks[index].correct
                    ? 'Juste'
                    : `Attendu : ${correction.blanks[index].expected}`}
                </span>
              )}
            </label>
          ))}
        </>
      ) : (
        <>
          <Markdown>{question.prompt}</Markdown>
          {choices.map(({ index, text }) => (
            <ChoiceOption
              key={index}
              name={question.cardId}
              text={text}
              type={question.multipleAnswers ? 'checkbox' : 'radio'}
              checked={draft.choices.includes(index)}
              onChange={() =>
                onChange({
                  ...draft,
                  choices: toggleChoice(draft.choices, index, question.multipleAnswers),
                })
              }
              outcome={
                correction
                  ? choiceOutcome(correction.correctChoices, index, draft.choices.includes(index))
                  : undefined
              }
            />
          ))}
        </>
      )}
      {correction && (
        <div
          className={`rounded-lg p-3 text-sm ${correction.correct ? 'bg-success-soft' : 'bg-danger-soft'}`}
        >
          <p className="font-medium">{correction.correct ? 'Juste !' : 'Pas tout à fait.'}</p>
          <Markdown>{correction.explanation}</Markdown>
        </div>
      )}
    </fieldset>
  )
}
