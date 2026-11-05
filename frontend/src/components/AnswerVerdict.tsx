import { CheckCircle, XCircle } from '@phosphor-icons/react'
import type { ReactNode } from 'react'

/**
 * Le verdict du serveur sur une réponse (ADR 0050) : une icône, une couleur et un petit mouvement, jamais la couleur
 * seule. Sans mouvement pour qui en demande moins.
 */
export function AnswerVerdict({ correct, children }: { correct: boolean; children: ReactNode }) {
  return (
    <p
      role="status"
      className={`flex items-center gap-2 font-semibold ${correct ? 'text-success motion-safe:animate-pop' : 'text-danger motion-safe:animate-shake'}`}
    >
      {correct ? (
        <CheckCircle weight="fill" aria-hidden className="size-6 shrink-0" />
      ) : (
        <XCircle weight="fill" aria-hidden className="size-6 shrink-0" />
      )}
      {children}
    </p>
  )
}
