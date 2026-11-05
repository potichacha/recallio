import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/errors'
import { useReportContent, type ReportReason } from '../api/reports'
import { REPORT_REASONS } from './reportReasons'
import { button } from './ui/button'

/** Ce qu'on signale : une carte, ou une page du Tour. */
export type ReportTarget = { cardId: string } | { subject: string; notion: string; page: number }

const MAX_COMMENT_LENGTH = 1000

/**
 * « Signaler une erreur » (ADR 0042) : le contenu est rédigé avec l'aide d'une IA, il aura des erreurs. Le parent donne
 * une clé par contenu, pour repartir d'un formulaire fermé à la carte ou à la page suivante.
 */
export function ReportButton({ target }: { target: ReportTarget }) {
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState<ReportReason>('WRONG_ANSWER')
  const [comment, setComment] = useState('')
  const report = useReportContent()

  if (report.isSuccess) {
    return (
      <p role="status" className="text-xs text-ink-muted">
        Merci : le signalement est envoyé, il sera relu.
      </p>
    )
  }
  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="self-start text-xs text-ink-muted underline hover:text-ink-soft"
      >
        Signaler une erreur
      </button>
    )
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    report.mutate({ ...target, reason, comment: comment.trim() || undefined })
  }

  return (
    <form
      onSubmit={submit}
      aria-label="Signaler une erreur"
      className="flex flex-col gap-3 rounded-lg border border-line bg-surface p-4 text-sm"
    >
      <fieldset className="flex flex-col gap-1">
        <legend className="mb-1 font-medium">Qu’est-ce qui ne va pas ?</legend>
        {REPORT_REASONS.map((option) => (
          <label key={option.reason} className="flex items-center gap-2">
            <input
              type="radio"
              name="report-reason"
              value={option.reason}
              checked={reason === option.reason}
              onChange={() => setReason(option.reason)}
            />
            {option.label}
          </label>
        ))}
      </fieldset>
      <label className="flex flex-col gap-1">
        <span className="font-medium">Précisions (facultatif)</span>
        <textarea
          value={comment}
          onChange={(event) => setComment(event.target.value)}
          maxLength={MAX_COMMENT_LENGTH}
          rows={3}
          className="rounded-lg border border-line-strong px-3 py-2"
        />
      </label>
      <div className="flex gap-2">
        <button type="submit" disabled={report.isPending} className={button({ size: 'sm' })}>
          Envoyer
        </button>
        <button
          type="button"
          onClick={() => setOpen(false)}
          className="rounded-lg px-3 py-1.5 font-medium text-ink-muted hover:bg-surface-muted"
        >
          Annuler
        </button>
      </div>
      {report.isError && (
        <p role="alert" className="text-danger">
          {report.error instanceof ApiError
            ? report.error.message
            : 'Le signalement n’a pas pu être envoyé.'}
        </p>
      )}
    </form>
  )
}
