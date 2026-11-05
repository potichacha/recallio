import type { CardType } from '../api/content'

type Draft = { label: string; echo: string; code: boolean; rows: number }

const ANSWER: Draft = {
  label: 'Votre réponse (facultatif)',
  echo: 'Votre réponse',
  code: false,
  rows: 3,
}

/** Ce que demande le brouillon de chaque format, et s'il s'écrit en code (police à chasse fixe, sans correcteur). */
const DRAFTS: Partial<Record<CardType, Draft>> = {
  OPEN_SCENARIO: { ...ANSWER, rows: 5 },
  CODE_OUTPUT: { ...ANSWER, label: 'Ce que le code affiche (facultatif)', code: true },
  BUG_FIX: {
    label: 'Votre correction (facultatif)',
    echo: 'Votre correction',
    code: true,
    rows: 5,
  },
}

const draftOf = (type: CardType) => DRAFTS[type] ?? ANSWER

/**
 * Au recto d'une carte à auto-évaluer, de quoi écrire sa réponse avant de la retourner, pour la comparer ensuite à
 * celle attendue (ADR 0048). C'est facultatif : y réfléchir suffit.
 */
export function AnswerDraft({
  type,
  value,
  onChange,
}: {
  type: CardType
  value: string
  onChange: (value: string) => void
}) {
  const draft = draftOf(type)
  return (
    <label className="flex flex-col gap-1">
      <span className="text-sm font-medium">{draft.label}</span>
      <textarea
        value={value}
        onChange={(event) => onChange(event.target.value)}
        rows={draft.rows}
        spellCheck={!draft.code}
        className={`rounded-lg border border-slate-300 px-3 py-2 ${draft.code ? 'font-mono text-sm' : ''}`}
      />
    </label>
  )
}

/** Au verso, la réponse écrite au recto, à côté de celle attendue ; rien si on n'a rien écrit. */
export function DraftEcho({ type, value }: { type: CardType; value: string }) {
  if (value.trim() === '') return null
  const draft = draftOf(type)
  return (
    <section aria-label={draft.echo} className="flex flex-col gap-1 rounded-lg bg-slate-50 p-3">
      <h3 className="text-xs font-medium tracking-wide text-slate-500 uppercase">{draft.echo}</h3>
      <p className={`whitespace-pre-wrap ${draft.code ? 'font-mono text-sm' : ''}`}>{value}</p>
    </section>
  )
}
