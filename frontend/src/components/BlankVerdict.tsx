import { CheckCircle, XCircle } from '@phosphor-icons/react'

/** Le verdict d'un trou d'un code à compléter : une icône et un texte, en plus de la couleur (ADR 0058). */
export function BlankVerdict({ correct, expected }: { correct: boolean; expected: string }) {
  const Icon = correct ? CheckCircle : XCircle
  return (
    <span
      className={`flex items-center gap-1 text-sm font-bold ${correct ? 'text-success' : 'text-danger'}`}
    >
      <Icon weight="fill" aria-hidden className="size-4 shrink-0" />
      {correct ? 'Juste' : `Attendu : ${expected}`}
    </span>
  )
}
