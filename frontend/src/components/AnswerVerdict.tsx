import { CheckCircle, XCircle } from '@phosphor-icons/react'
import type { ReactNode } from 'react'
import { Rico } from './Rico'

/**
 * Le verdict du serveur sur une réponse (ADR 0050, 0058) : Rico applaudit ou réfléchit, à côté d'une icône, d'une
 * couleur et d'un petit mouvement ; jamais la couleur seule. Sans mouvement pour qui en demande moins.
 *
 * `announce` le fait lire aussitôt par un lecteur d'écran : vrai pour la carte à laquelle on vient de répondre, faux sur
 * une page qui corrige plusieurs questions à la fois (examen blanc, validation d'une notion), où tout parlerait en
 * même temps.
 */
export function AnswerVerdict({
  correct,
  announce = true,
  children,
}: {
  correct: boolean
  announce?: boolean
  children: ReactNode
}) {
  return (
    <div className="flex items-center gap-3">
      <Rico mood={correct ? 'cheering' : 'thinking'} size={48} decorative />
      <p
        role={announce ? 'status' : undefined}
        className={`flex items-center gap-2 font-extrabold ${correct ? 'text-success motion-safe:animate-pop' : 'text-danger motion-safe:animate-shake'}`}
      >
        {correct ? (
          <CheckCircle weight="fill" aria-hidden className="size-6 shrink-0" />
        ) : (
          <XCircle weight="fill" aria-hidden className="size-6 shrink-0" />
        )}
        {children}
      </p>
    </div>
  )
}
