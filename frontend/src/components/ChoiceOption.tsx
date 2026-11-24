import { CheckCircle, XCircle, type Icon } from '@phosphor-icons/react'
import type { ChoiceOutcome } from './choices'
import { Markdown } from './Markdown'

/**
 * Avant la correction : un contour à 3 de contraste, et un choix sélectionné qui change de fond et de contour en plus de
 * sa pastille cochée. Le focus du clavier entoure toute la tuile, pas seulement sa pastille.
 */
const UNANSWERED =
  'border-line-strong bg-surface transition-transform duration-100 hover:border-brand has-checked:translate-y-0.5 has-checked:border-brand has-checked:bg-brand-soft motion-reduce:transition-none has-focus-visible:outline-3 has-focus-visible:outline-offset-2 has-focus-visible:outline-brand'

/** Après la correction : chaque verdict a sa couleur, son icône et son texte, jamais la couleur seule (ADR 0058). */
const OUTCOMES: Record<
  ChoiceOutcome,
  { style: string; label?: string; icon?: Icon; iconStyle?: string }
> = {
  correct: {
    style: 'border-success bg-success-soft',
    label: 'Bonne réponse',
    icon: CheckCircle,
    iconStyle: 'text-success',
  },
  missed: {
    style: 'border-dashed border-success bg-surface',
    label: 'Bonne réponse, non cochée',
    icon: CheckCircle,
    iconStyle: 'text-success',
  },
  wrong: {
    style: 'border-danger bg-danger-soft',
    label: 'Faux',
    icon: XCircle,
    iconStyle: 'text-danger',
  },
  neutral: { style: 'border-line-strong bg-surface' },
}

/**
 * La pastille d'un choix, dessinée avec les tokens : un cercle pour une seule réponse, un carré arrondi pour plusieurs.
 * Cochée, elle se remplit à la couleur de la marque autour d'un point clair. Le contrôle reste celui du navigateur
 * (clavier, lecteur d'écran) : seul son dessin change.
 */
const MARK =
  'size-[1.375rem] shrink-0 appearance-none border-2 border-line-strong bg-surface outline-none checked:border-[7px] checked:border-brand disabled:opacity-70'

/**
 * Un choix de QCM (ADR 0058) : une grande tuile au bord épais en dessous, avec son bouton radio ou sa case à cocher,
 * puis, après correction, son verdict. Plusieurs questions sur une même page ont chacune leur `name`, pour que leurs
 * boutons radio ne forment pas un seul groupe.
 */
export function ChoiceOption({
  text,
  type,
  checked,
  onChange,
  outcome,
  disabled = false,
  name = 'choice',
}: {
  text: string
  type: 'radio' | 'checkbox'
  checked: boolean
  onChange: () => void
  outcome?: ChoiceOutcome
  disabled?: boolean
  name?: string
}) {
  const verdict = outcome && OUTCOMES[outcome]
  const VerdictIcon = verdict?.icon
  return (
    <label
      className={`flex min-h-14 items-center gap-3 rounded-xl border-2 border-b-[5px] px-4 py-2.5 ${verdict ? verdict.style : UNANSWERED}`}
    >
      <input
        type={type}
        name={name}
        checked={checked}
        onChange={onChange}
        disabled={disabled || outcome !== undefined}
        className={`${MARK} ${type === 'radio' ? 'rounded-full' : 'rounded-md'}`}
      />
      <span className="flex flex-1 flex-col">
        <Markdown tone="choice">{text}</Markdown>
        {verdict?.label && (
          <span className="text-xs font-extrabold text-ink-muted">{verdict.label}</span>
        )}
      </span>
      {VerdictIcon && (
        <VerdictIcon weight="fill" aria-hidden className={`size-6 shrink-0 ${verdict.iconStyle}`} />
      )}
    </label>
  )
}
