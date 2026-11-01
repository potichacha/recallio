import type { ChoiceOutcome } from './choices'
import { Markdown } from './Markdown'

const OUTCOME_STYLES: Record<ChoiceOutcome, string> = {
  correct: 'border-emerald-300 bg-emerald-50',
  missed: 'border-emerald-300 bg-white',
  wrong: 'border-rose-300 bg-rose-50',
  neutral: 'border-slate-200 bg-white',
}

const OUTCOME_LABELS: Record<ChoiceOutcome, string | undefined> = {
  correct: 'Bonne réponse',
  missed: 'Bonne réponse, non cochée',
  wrong: 'Faux',
  neutral: undefined,
}

/** Un choix de QCM : bouton radio ou case à cocher, puis, après correction, son verdict. */
export function ChoiceOption({
  text,
  type,
  checked,
  onChange,
  outcome,
  disabled = false,
}: {
  text: string
  type: 'radio' | 'checkbox'
  checked: boolean
  onChange: () => void
  outcome?: ChoiceOutcome
  disabled?: boolean
}) {
  const style = outcome
    ? OUTCOME_STYLES[outcome]
    : 'border-slate-200 bg-white hover:border-indigo-300'
  const label = outcome && OUTCOME_LABELS[outcome]
  return (
    <label className={`flex items-start gap-3 rounded-lg border px-3 py-2 ${style}`}>
      <input
        type={type}
        name="choice"
        checked={checked}
        onChange={onChange}
        disabled={disabled || outcome !== undefined}
        className="mt-1"
      />
      <span className="flex flex-1 flex-col">
        <Markdown>{text}</Markdown>
        {label && <span className="text-xs font-medium text-slate-600">{label}</span>}
      </span>
    </label>
  )
}
